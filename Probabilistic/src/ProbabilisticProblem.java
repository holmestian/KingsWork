import javafx.util.Pair;

import java.util.*;

/**
 * Created by gejing on 2/27/16.
 */
public class ProbabilisticProblem {
    private double[][] T;  //transition model
    private double[][] Or; //sensor model for red
    private double[][] Og; //sensor model for green
    private double[][] Ob; //sensor model for blue
    private double[][] Oy; //sensor model for yellow

    private Maze maze;
    private int tileNumber;

    Map<Integer, Pair<Integer, Integer>> index = new HashMap<>();

    private List<Pair<Integer, Integer>> actualPath = new ArrayList<>(); //record actual path the robot past.
    private double[][] probabilityDistribution; //probability distribution of each state.

    public ProbabilisticProblem() {
        maze = Maze.readFromFile("simple.maz");
        if (maze == null) throw new RuntimeException("maze is null");
        int indexNumber = 0;
        for (int y = 0; y < maze.height; y++) {
            for (int x = 0; x < maze.width; x++) {
                if (maze.isLegal(x, y)) {
                    indexNumber++;
                    index.put(indexNumber, new Pair<>(x, y));
                }
            }
        }
        tileNumber = indexNumber;

        T = new double[indexNumber][];
        Ob = new double[indexNumber][];
        Og = new double[indexNumber][];
        Or = new double[indexNumber][];
        Oy = new double[indexNumber][];

        for (int i = 0; i < indexNumber; i++) {
            Pair<Integer, Integer> position1 = index.get(i + 1);
            T[i] = new double[indexNumber];
            Or[i] = new double[indexNumber];
            Og[i] = new double[indexNumber];
            Ob[i] = new double[indexNumber];
            Oy[i] = new double[indexNumber];

            for (int j = 0; j < indexNumber; j++) {
                //generate transition model:
                Pair<Integer, Integer> position2 = index.get(j + 1);
                T[i][j] = getTransitionProbability(position1, position2);
                //generate sensor model for colors red, green, blue, yellow:
                if (i == j) {
                    Or[i][j] = getSensorProbability(position1, 'r');
                    Og[i][j] = getSensorProbability(position1, 'g');
                    Ob[i][j] = getSensorProbability(position1, 'b');
                    Oy[i][j] = getSensorProbability(position1, 'y');
                } else {
                    Or[i][j] = 0;
                    Og[i][j] = 0;
                    Ob[i][j] = 0;
                    Oy[i][j] = 0;
                }
            }
        }
    }

    public void doMoves(int[][] steps) {
        probabilityDistribution = new double[steps.length + 1][];
        double[] firstDistribution = new double[tileNumber];
        for (int i = 0; i < tileNumber; i++) {
            firstDistribution[i] = 1.0 / tileNumber;
        }
        probabilityDistribution[0] = firstDistribution;
        //choose initial position random:
        int initPosition = new Random().nextInt(tileNumber) + 1;
        Pair<Integer, Integer> position = index.get(initPosition);
        actualPath.add(position);

        char[] sensorReadings = new char[steps.length];

        for (int i = 0; i < steps.length; i++) {
            System.out.println("step " + i);

            if (maze.isLegal(position.getKey() + steps[i][0], position.getValue() + steps[i][1])) {
                position = new Pair<>(position.getKey() + steps[i][0], position.getValue() + steps[i][1]);
            }
            actualPath.add(position);
            sensorReadings[i] = getTileColor(position);
            probabilityDistribution[i + 1] = forward(probabilityDistribution[i], getTileColor(position));
        }

        //test
        double[][] s = forwardBackward(firstDistribution, sensorReadings);
        System.out.println("the smooth for each step:");
        for (int i = 0; i < s.length; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < s[0].length; j++) {
                sb.append(s[i][j]);
                sb.append(" , ");
            }
            System.out.println(sb.toString());
        }
    }

    public void printInfo() {
        System.out.println(maze);
        System.out.println("the actual path robot move is:");
        for (Pair<Integer, Integer> p : actualPath) {
            System.out.print("(" + p.getKey() + "," + p.getValue() + ")");
        }
        System.out.println();
        System.out.println("the distribution for each step:");
        for (int i = 0; i < probabilityDistribution.length; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < probabilityDistribution[0].length; j++) {
                sb.append(probabilityDistribution[i][j]);
                sb.append(" , ");
            }
            System.out.println(sb.toString());
        }
    }

    private double[][] forwardBackward(double[] prior, char[] sensorReadings){
        double[][] forwardDistribution = new double[sensorReadings.length+1][];
        double[][] smoothDistribution = new double[sensorReadings.length+1][];
        double[] backwardMessage = new double[prior.length];

        //initial forward msg and backward msg:
        forwardDistribution[0] = prior;
        for (int i = 0; i < backwardMessage.length; i++) {
            backwardMessage[i] = 1;
        }

        for (int i = 0; i < sensorReadings.length; i++) {
            forwardDistribution[i+1] = forward(forwardDistribution[i], sensorReadings[i]);
        }

        for (int i = sensorReadings.length; i > 0; i--) {
            smoothDistribution[i] = MatrixTools.vectorNormalize(MatrixTools.vectorMultiple(forwardDistribution[i], backwardMessage));
            backwardMessage = backward(backwardMessage, sensorReadings[i-1]);
        }
        smoothDistribution[0] = new double[prior.length];
        return smoothDistribution;
    }

    private double[] forward(double[] lastOne, char color) {
        double[][] O;
        switch (color){
            case 'r':
                O = Or;
                break;
            case 'g':
                O = Og;
                break;
            case 'b':
                O = Ob;
                break;
            case 'y':
                O = Oy;
                break;
            default:
                throw new RuntimeException("wrong color");
        }
        double[][] line = new double[1][];
        line[0] = lastOne;
        double[][] result = MatrixTools.multiple(MatrixTools.multiple(O, MatrixTools.transpose(T)), MatrixTools.transpose(line));
        return MatrixTools.transpose(MatrixTools.normalize(result))[0];
    }

    private double[] backward(double[] bv, char color){
        double[][] O;
        switch (color){
            case 'r':
                O = Or;
                break;
            case 'g':
                O = Og;
                break;
            case 'b':
                O = Ob;
                break;
            case 'y':
                O = Oy;
                break;
            default:
                throw new RuntimeException("wrong color");
        }
        double[][] line = new double[1][];
        line[0] = bv;
        double[][] result = MatrixTools.multiple(MatrixTools.multiple(T,O), MatrixTools.transpose(line));
        return MatrixTools.transpose(result)[0];
    }

    private double getTransitionProbability(Pair<Integer, Integer> p1, Pair<Integer, Integer> p2) {
        int x1 = p1.getKey(),
                y1 = p1.getValue(),
                x2 = p2.getKey(),
                y2 = p2.getValue();
        int walls = 0;
        if (!maze.isLegal(x1 + 1, y1)) walls++;
        if (!maze.isLegal(x1 - 1, y1)) walls++;
        if (!maze.isLegal(x1, y1 + 1)) walls++;
        if (!maze.isLegal(x1, y1 - 1)) walls++;

        if (Math.abs(x2 - x1 + y2 - y1) == 1) return 0.25;
        if (p1.equals(p2)) return walls / 4.0;
        return 0;
    }

    private double getSensorProbability(Pair<Integer, Integer> position, char color) {
        int x = position.getKey();
        int y = position.getValue();
        char trueColor = maze.getChar(x, y);
        if (trueColor == color) {
            return 0.88;
        }
        return 0.04;
    }

    private char getTileColor(Pair<Integer, Integer> position) {
        return maze.getChar(position.getKey(), position.getValue());
    }

    public static void main(String[] args) {
        ProbabilisticProblem probabilisticProblem = new ProbabilisticProblem();
        int[][] steps = {Maze.EAST, Maze.SOUTH, Maze.WEST, Maze.WEST, Maze.NORTH};
        probabilisticProblem.doMoves(steps);
        probabilisticProblem.printInfo();
    }
}
