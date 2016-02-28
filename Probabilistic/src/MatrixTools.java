/**
 * Created by gejing on 2/27/16.
 */
public class MatrixTools {
    public static double[][] multiple(double[][] ma, double[][] mb) {
        if (isLegal(ma, mb)) {
            double[][] result = new double[ma.length][mb[0].length];
            for (int i = 0; i < ma.length; i++) {
                for (int j = 0; j < mb[0].length; j++) {
                    result[i][j] = getValue(ma, mb, i, j);
                }
            }
            return result;
        }
        return null;
    }

    public static double[][] transpose(double[][] matrix) {
        int m = matrix.length;  //rows
        int n = matrix[0].length;//cols

        double[][] transMatrix = new double[n][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                transMatrix[j][i] = matrix[i][j];
            }
        }
        return transMatrix;
    }

    public static double[][] normalize(double[][] matrix) {
        if (matrix.length > 1 && matrix[0].length > 1)
            return null;
        double sum = 0;
        double[][] result = new double[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                sum += matrix[i][j];
            }
        }
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                result[i][j] = matrix[i][j] / sum;
            }
        }
        return result;
    }

    public static double[] vectorMultiple(double[] a, double[] b) {
        if (a.length != b.length) {
            return null;
        }
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] * b[i];
        }
        return result;
    }

    public static double[] vectorNormalize(double[] m) {
        double sum = 0;
        double[] result = new double[m.length];
        for (int i = 0; i < m.length; i++) {
            sum += m[i];
        }
        double test = 0.0 / sum;
        if (Double.isNaN(test)) {
            return m;
        }
        for (int i = 0; i < m.length; i++) {
            result[i] = m[i] / sum;
        }
        return result;
    }

    private static boolean isLegal(double[][] a, double[][] b) {
        return a[0].length == b.length;
    }

    private static double getValue(double[][] a, double[][] b, int row, int col) {
        double result = 0;
        for (int i = 0; i < a[0].length; i++) {
            result += a[row][i] * b[i][col];
        }
        return result;
    }

    public static void main(String[] args) {
        double[][] test = {{1, 2, 3}, {4, 5, 6}};
        double[][] test2 = {{1}, {2}, {3}};
        double[][] ans = multiple(test, test2);
        double[][] t = transpose(test);
        double[][] n = normalize(test2);
        double[] a = {0, 0, 0};
        double[] b = {4, 5, 6};
        double[] v = vectorMultiple(a, b);
        double[] nn = vectorNormalize(a);
    }
}
