package school.hei.haapi.service.utils;

import static school.hei.haapi.service.utils.DataFormatterUtils.formatLocalDate;
import static school.hei.haapi.service.utils.DataFormatterUtils.instantToOcsDateFormat;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class XlsxCellsGenerator<T> implements BiFunction<List<T>, List<String>, byte[]> {

  /***
   *
   * @param objectsToPrint the first function argument, given List object will be printed in the xlsx
   * @param fieldsToBePrinted the second function argument, given List of Class fields to be printed
   * @return xlsx file
   */
  @Override
  @SneakyThrows
  public byte[] apply(List<T> objectsToPrint, List<String> fieldsToBePrinted) {
    Workbook workbook = new XSSFWorkbook();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    Sheet sheet = workbook.createSheet("excel-sheet");
    Row row0 = sheet.createRow(0);

    generateHeadedRow(row0, fieldsToBePrinted);

    for (int i = 0; i < objectsToPrint.size(); i++) {
      Row rowi = sheet.createRow(i + 1);
      generateBodiesRow(rowi, objectsToPrint.get(i), fieldsToBePrinted);
    }

    try {
      workbook.write(bos);
    } finally {
      bos.close();
    }
    return bos.toByteArray();
  }

  @SneakyThrows
  private void generateBodiesRow(Row rowi, T objectToPrint, List<String> fieldsToBePrinted) {
    for (int i = 0; i < fieldsToBePrinted.size(); i++) {
      Object value = getNestedFieldValue(objectToPrint, fieldsToBePrinted.get(i));
      String formattedValue = formatValue(value);
      rowi.createCell(i).setCellValue(formattedValue != null ? formattedValue : "");
    }
  }

  @SneakyThrows
  private Object getNestedFieldValue(Object obj, String fieldPath) {
    String[] fields = fieldPath.split("\\.");
    Object currentObject = obj;

    for (String fieldName : fields) {
      var field = currentObject.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      currentObject = field.get(currentObject);

      if (currentObject == null) break;
    }

    return currentObject;
  }

  private void generateHeadedRow(Row row0, List<String> fieldsToBePrinted) {
    for (int i = 0; i < fieldsToBePrinted.size(); i++) {
      row0.createCell(i).setCellValue(fieldsToBePrinted.get(i).toString());
    }
  }

  private String formatValue(Object value) {
    if (value instanceof LocalDate) {
      return formatLocalDate((LocalDate) value);
    } else if (value instanceof Instant) {
      return instantToOcsDateFormat((Instant) value);
    } else {
      return value != null ? value.toString() : null;
    }
  }
}
