package icepackai;

/** Prints rows as a plain-text table with a header row. */
public class TextTable {
  private final String[] columnNames;
  private final Object[][] data;

  public TextTable(String[] columnNames, Object[][] data) {
    this.columnNames = columnNames;
    this.data = data;
  }

  public void printTable() {
    int[] widths = new int[columnNames.length];
    for (int c = 0; c < columnNames.length; c++) {
      widths[c] = columnNames[c].length();
    }
    for (Object[] row : data) {
      for (int c = 0; c < widths.length && row != null && c < row.length; c++) {
        widths[c] = Math.max(widths[c], String.valueOf(row[c]).length());
      }
    }
    int total = 1;
    for (int w : widths) {
      total += w + 3;
    }
    System.out.println(repeat('_', total));
    printRow(columnNames, widths);
    System.out.println("|" + repeat('=', total - 2) + "|");
    for (Object[] row : data) {
      printRow(row, widths);
    }
  }

  private static void printRow(Object[] row, int[] widths) {
    StringBuilder sb = new StringBuilder("|");
    for (int c = 0; c < widths.length; c++) {
      String cell = (row != null && c < row.length) ? String.valueOf(row[c]) : "";
      sb.append(' ').append(cell).append(repeat(' ', widths[c] - cell.length())).append(" |");
    }
    System.out.println(sb);
  }

  private static String repeat(char ch, int n) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < n; i++) {
      sb.append(ch);
    }
    return sb.toString();
  }
}
