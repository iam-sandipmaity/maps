package app.organicmaps.telecom.importer;

import androidx.annotation.NonNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ImportSummary implements Serializable {

  private static final long serialVersionUID = 1L;

  private int totalProcessed;
  private int importedCount;
  private int updatedCount;
  private int skippedCount;
  private int invalidCount;
  private final List<String> validationErrors = new ArrayList<>();

  public void incrementTotal() {
    totalProcessed++;
  }

  public void incrementImported() {
    importedCount++;
  }

  public void incrementUpdated() {
    updatedCount++;
  }

  public void incrementSkipped() {
    skippedCount++;
  }

  public void incrementInvalid() {
    invalidCount++;
  }

  public void addValidationError(@NonNull String error) {
    if (validationErrors.size() < 100) { // Keep top 100 to avoid memory ballooning
      validationErrors.add(error);
    }
  }

  public int getTotalProcessed() {
    return totalProcessed;
  }

  public int getImportedCount() {
    return importedCount;
  }

  public int getUpdatedCount() {
    return updatedCount;
  }

  public int getSkippedCount() {
    return skippedCount;
  }

  public int getInvalidCount() {
    return invalidCount;
  }

  @NonNull
  public List<String> getValidationErrors() {
    return validationErrors;
  }

  @NonNull
  public String getFormattedSummary() {
    StringBuilder sb = new StringBuilder();
    sb.append("Total Rows Processed: ").append(totalProcessed).append("\n");
    sb.append("Successfully Imported: ").append(importedCount).append("\n");
    sb.append("Updated Existing: ").append(updatedCount).append("\n");
    sb.append("Skipped: ").append(skippedCount).append("\n");
    sb.append("Invalid Records: ").append(invalidCount).append("\n");
    if (!validationErrors.isEmpty()) {
      sb.append("\nValidation Errors (Top ").append(validationErrors.size()).append("):\n");
      for (int i = 0; i < validationErrors.size(); i++) {
        sb.append("• ").append(validationErrors.get(i)).append("\n");
      }
    }
    return sb.toString();
  }
}
