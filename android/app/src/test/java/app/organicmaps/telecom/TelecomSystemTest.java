package app.organicmaps.telecom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import app.organicmaps.telecom.importer.ColumnMapper;
import app.organicmaps.telecom.importer.CsvParser;
import app.organicmaps.telecom.importer.ImportSummary;
import app.organicmaps.telecom.importer.TelecomCsvExporter;
import app.organicmaps.telecom.model.TelecomSite;
import app.organicmaps.telecom.planner.VisitPlannerManager;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TelecomSystemTest {

  @Test
  public void testCsvParserBasicAndQuotesAndMultiline() throws IOException {
    String csv = "site_id,site_name,notes\n"
        + "IWB-KGPU-ENB-9036,\"Mohammadpur, Kharagpur\",\"Site located near water tank.\nKey with guard.\"\n"
        + "IWB-MDPR-ENB-9001,\"Midnapore Tower\",\"Contains \"\"dual\"\" DG\"\n";

    List<List<String>> rows = CsvParser.parse(csv);
    assertEquals(3, rows.size());

    // Row 1
    List<String> header = rows.get(0);
    assertEquals("site_id", header.get(0));
    assertEquals("site_name", header.get(1));
    assertEquals("notes", header.get(2));

    // Row 2
    List<String> row1 = rows.get(1);
    assertEquals("IWB-KGPU-ENB-9036", row1.get(0));
    assertEquals("Mohammadpur, Kharagpur", row1.get(1));
    assertTrue(row1.get(2).contains("water tank"));
    assertTrue(row1.get(2).contains("Key with guard"));

    // Row 3
    List<String> row2 = rows.get(2);
    assertEquals("IWB-MDPR-ENB-9001", row2.get(0));
    assertEquals("Midnapore Tower", row2.get(1));
    assertEquals("Contains \"dual\" DG", row2.get(2));
  }

  @Test
  public void testCoordinateValidation() {
    // Valid coordinates
    assertTrue(TelecomSite.validateCoordinates(22.346500, 87.231200));
    assertTrue(TelecomSite.validateCoordinates(-33.8688, 151.2093));
    assertTrue(TelecomSite.validateCoordinates(90.0, 180.0));
    assertTrue(TelecomSite.validateCoordinates(-90.0, -180.0));

    // Invalid coordinates
    assertFalse(TelecomSite.validateCoordinates(91.0, 50.0));
    assertFalse(TelecomSite.validateCoordinates(-90.1, 50.0));
    assertFalse(TelecomSite.validateCoordinates(22.0, 181.0));
    assertFalse(TelecomSite.validateCoordinates(22.0, -180.5));
    assertFalse(TelecomSite.validateCoordinates(0.0, 0.0)); // Null point
    assertFalse(TelecomSite.validateCoordinates(Double.NaN, 50.0));
    assertFalse(TelecomSite.validateCoordinates(22.0, Double.POSITIVE_INFINITY));
  }

  @Test
  public void testColumnMapperAutoDetectAndRowMapping() {
    List<String> headers = List.of(
        "SAP ID", "SITE NAME", "Lat", "Long", "JC NAME", "Technician", "TECHNICIAN Mobile Number", "SMPS Make"
    );

    ColumnMapper mapper = ColumnMapper.autoDetectMapping(headers);
    assertEquals(ColumnMapper.FIELD_SITE_ID, mapper.getMappings().get(0));
    assertEquals(ColumnMapper.FIELD_SITE_NAME, mapper.getMappings().get(1));
    assertEquals(ColumnMapper.FIELD_LATITUDE, mapper.getMappings().get(2));
    assertEquals(ColumnMapper.FIELD_LONGITUDE, mapper.getMappings().get(3));
    assertEquals(ColumnMapper.FIELD_LOCATION, mapper.getMappings().get(4));
    assertEquals(ColumnMapper.FIELD_TECH_NAME, mapper.getMappings().get(5));
    assertEquals(ColumnMapper.FIELD_TECH_PHONE, mapper.getMappings().get(6));
    assertEquals(ColumnMapper.FIELD_SMPS_MAKE, mapper.getMappings().get(7));

    ImportSummary summary = new ImportSummary();
    List<String> row = List.of(
        "I-WB-NDRT-ENB-9009", "NANDAPUR", "22.089841", "87.889908", "HALDIA", "AMALESH MONDAL", "06289037005", "Delta"
    );

    TelecomSite site = mapper.mapRow(row, 2, summary);
    assertNotNull(site);
    assertEquals("I-WB-NDRT-ENB-9009", site.getSiteId());
    assertEquals("NANDAPUR", site.getSiteName());
    assertEquals(22.089841, site.getLatitude(), 0.000001);
    assertEquals(87.889908, site.getLongitude(), 0.000001);
    assertTrue(site.hasValidCoords());
    assertEquals("HALDIA", site.getLocation());
    assertEquals("AMALESH MONDAL", site.getTechnicianName());
    assertEquals("06289037005", site.getTechnicianPhone()); // Preserves leading zero
    assertEquals("Delta", site.getSmpsMake());
  }

  @Test
  public void testMissingSiteIdAndInvalidCoordinatesReporting() {
    List<String> headers = List.of("Site ID", "Latitude", "Longitude");
    ColumnMapper mapper = ColumnMapper.autoDetectMapping(headers);
    ImportSummary summary = new ImportSummary();

    // Missing Site ID
    List<String> badRow1 = List.of("", "22.0", "87.0");
    TelecomSite site1 = mapper.mapRow(badRow1, 1, summary);
    assertEquals(null, site1);
    assertEquals(1, summary.getInvalidCount());

    // Invalid Coordinates
    List<String> badRow2 = List.of("IWB-KGP-9999", "1022.0", "87.0");
    TelecomSite site2 = mapper.mapRow(badRow2, 2, summary);
    assertNotNull(site2);
    assertFalse(site2.hasValidCoords()); // Should not crash, accepts record with invalid coords flagged
    assertFalse(summary.getValidationErrors().isEmpty());
  }

  @Test
  public void testVisitPlannerDistanceAndSequenceOptimization() {
    // 3 sites along a line: A (22.0, 87.0), B (22.2, 87.0), C (22.4, 87.0)
    TelecomSite siteA = new TelecomSite("SITE-A");
    siteA.setCoordinates(22.0, 87.0);

    TelecomSite siteB = new TelecomSite("SITE-B");
    siteB.setCoordinates(22.2, 87.0);

    TelecomSite siteC = new TelecomSite("SITE-C");
    siteC.setCoordinates(22.4, 87.0);

    // Distance calculation
    double distAB = VisitPlannerManager.calculateHaversineDistanceKm(22.0, 87.0, 22.2, 87.0);
    assertTrue(distAB > 20.0 && distAB < 25.0); // ~22.2 km for 0.2 deg lat

    double distAC = VisitPlannerManager.calculateHaversineDistanceKm(22.0, 87.0, 22.4, 87.0);
    assertTrue(distAC > distAB);
  }

  @Test
  public void testCsvExportRoundTrip() throws IOException {
    List<TelecomSite> sites = new ArrayList<>();

    TelecomSite s1 = new TelecomSite("I-WB-BLDA-AG1-9003");
    s1.setSiteName("RABINDRANAGAR Road");
    s1.setSiteType("AG1");
    s1.setLocation("Kharagpur");
    s1.setCoordinates(22.06887, 87.33884);
    s1.setTechnicianName("RANJIT SEN");
    s1.setTechnicianPhone("9734686679");
    s1.setSmpsMake("Delta");
    s1.setDgMake("Eicher");
    s1.setNotes("Regular monthly maintenance completed");
    s1.addExtraAttribute("CUSTOM_TOWER_HEIGHT", "40m");
    sites.add(s1);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    TelecomCsvExporter.exportToCsv(sites, baos);

    byte[] csvBytes = baos.toByteArray();
    assertTrue(csvBytes.length > 0);

    List<List<String>> parsedRows = CsvParser.parse(new ByteArrayInputStream(csvBytes));
    assertEquals(2, parsedRows.size()); // Header + 1 row

    ColumnMapper mapper = ColumnMapper.autoDetectMapping(parsedRows.get(0));
    ImportSummary summary = new ImportSummary();
    TelecomSite reimported = mapper.mapRow(parsedRows.get(1), 2, summary);

    assertNotNull(reimported);
    assertEquals("I-WB-BLDA-AG1-9003", reimported.getSiteId());
    assertEquals("RABINDRANAGAR Road", reimported.getSiteName());
    assertEquals("AG1", reimported.getSiteType());
    assertEquals(22.06887, reimported.getLatitude(), 0.00001);
    assertEquals(87.33884, reimported.getLongitude(), 0.00001);
    assertEquals("RANJIT SEN", reimported.getTechnicianName());
    assertEquals("9734686679", reimported.getTechnicianPhone());
    assertEquals("Delta", reimported.getSmpsMake());
    assertEquals("Eicher", reimported.getDgMake());
    assertEquals("Regular monthly maintenance completed", reimported.getNotes());
  }

  @Test
  public void testPerformance5000Records() {
    long startTime = System.currentTimeMillis();

    List<String> headers = List.of("Site ID", "Site Name", "Lat", "Long", "Location", "Technician", "Mobile");
    ColumnMapper mapper = ColumnMapper.autoDetectMapping(headers);
    ImportSummary summary = new ImportSummary();

    List<TelecomSite> sites = new ArrayList<>(5000);
    for (int i = 0; i < 5000; i++) {
      List<String> row = List.of(
          "IWB-KGP-ENB-" + (1000 + i),
          "Site " + i,
          String.valueOf(22.0 + (i * 0.0001)),
          String.valueOf(87.0 + (i * 0.0001)),
          "Kharagpur",
          "Tech " + (i % 20),
          "98000" + String.format("%05d", i)
      );
      TelecomSite site = mapper.mapRow(row, i + 1, summary);
      if (site != null) {
        sites.add(site);
      }
    }

    long elapsed = System.currentTimeMillis() - startTime;
    assertEquals(5000, sites.size());
    assertEquals(0, summary.getInvalidCount());
    assertTrue("Processing 5,000 records should take less than 1.5s, took: " + elapsed + "ms", elapsed < 1500);
  }

  @Test
  public void testXlsxColumnRefToIndex() {
    assertEquals(0, app.organicmaps.telecom.importer.XlsxParser.columnRefToIndex("A1"));
    assertEquals(1, app.organicmaps.telecom.importer.XlsxParser.columnRefToIndex("B2"));
    assertEquals(25, app.organicmaps.telecom.importer.XlsxParser.columnRefToIndex("Z10"));
    assertEquals(26, app.organicmaps.telecom.importer.XlsxParser.columnRefToIndex("AA5"));
    assertEquals(27, app.organicmaps.telecom.importer.XlsxParser.columnRefToIndex("AB1"));
    assertEquals(46, app.organicmaps.telecom.importer.XlsxParser.columnRefToIndex("AU1"));
  }

  @Test
  public void testInfraTrackerCompositeHeaderMapping() {
    List<String> headers = List.of(
        "SL No.", "CMP", "SAP ID", "JC Name", "Site Name", "Address", "Lat", "Long",
        "Sites Scope", "Power Status", "Solar Site(Yes/No)", "Facility Type", "Technician Name",
        "Techinician Number", "EB availibilty", "Site Load(Amp)", "SMPS Make", "SMPS Rating",
        "Fault details", "Remarks", "BM Make", "BM Rating", "BB backup", "BM Physical Count",
        "BM Faulty Count", "BM Remarks", "RM Make", "RM Rating", "RM Physical Count",
        "RM Bypass Count", "RM Faulty Count", "RM Remarks", "DG Make", "DG Rating",
        "GCU Make", "GCU Model", "GCU Faulty/GCU Working", "Fuel sensor status",
        "automation status", "DGB/CD", "STATUS", "Solar MPPT Make", "MPPT Rating",
        "Total MPPT count", "MPPT Faulty Count", "Other remarks", "VEGETATION STATUS"
    );

    ColumnMapper mapper = ColumnMapper.autoDetectMapping(headers);
    assertEquals("site_id", mapper.getMappings().get(2)); // SAP ID
    assertEquals("latitude", mapper.getMappings().get(6)); // Lat
    assertEquals("longitude", mapper.getMappings().get(7)); // Long
    assertEquals("technician_name", mapper.getMappings().get(12));
    assertEquals("technician_phone", mapper.getMappings().get(13));
    assertEquals("eb_availability", mapper.getMappings().get(14));
    assertEquals("smps_make", mapper.getMappings().get(16));
    assertEquals("bm_make", mapper.getMappings().get(20));
    assertEquals("rm_make", mapper.getMappings().get(26));
    assertEquals("dg_make", mapper.getMappings().get(32));

    List<String> sampleRow = new ArrayList<>(headers.size());
    for (int i = 0; i < headers.size(); i++) sampleRow.add("");
    sampleRow.set(2, "I-WB-NDRT-ENB-9009");
    sampleRow.set(4, "NANDAPUR");
    sampleRow.set(6, "22.089841");
    sampleRow.set(7, "87.889908");
    sampleRow.set(12, "AMALESH MONDAL");
    sampleRow.set(13, "6289037005");

    ImportSummary summary = new ImportSummary();
    TelecomSite site = mapper.mapRow(sampleRow, 1, summary);
    assertNotNull(site);
    assertEquals("I-WB-NDRT-ENB-9009", site.getSiteId());
    assertEquals("NANDAPUR", site.getSiteName());
    assertEquals(22.089841, site.getLatitude(), 0.00001);
    assertEquals(87.889908, site.getLongitude(), 0.00001);
    assertTrue(site.hasValidCoordinates());
    assertEquals("AMALESH MONDAL", site.getTechnicianName());
    assertEquals("6289037005", site.getTechnicianPhone());
  }
}
