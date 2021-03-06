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
    private List<Pair<Integer, Integer>> likelyPath = new ArrayList<>(); //record most likely path the robot past.
    private List<Pair<Integer, Integer>> viterbiPath = new ArrayList<>(); //record most likely path the robot past.
    private double[][] probabilityDistribution;                          //probability distribution of each state.
    private double[][] smoothyDistribution;                              //probability distribution generated by forward-backward algorithm.

    /**
     * do the follow things:
     * 1. get maze, count the number of tiles.
     * 2. generate the matrix of transition model and sensor model.
     */
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

    /**
     * Take the motions as input, and choose a random position for robot.
     * Then robot will move follow the order, generate sensor reading,
     * and calculate the filtering distribution, smoothy distribution and viterbi path.
     *
     * @param steps motions for robot.
     */
    public void doMoves(int[][] steps) {
        probabilityDistribution = new double[steps.length + 1][];
        double[] firstDistribution = new double[tileNumber];
        for (int i = 0; i < tileNumber; i++) {
            firstDistribution[i] = 1.0 / tileNumber;
        }
        probabilityDistribution[0] = firstDistribution;

        //choose initial position randomly:
        int initPosition = new Random().nextInt(tileNumber) + 1;
        Pair<Integer, Integer> position = index.get(initPosition);
        actualPath.add(position);

        char[] sensorReadings = new char[steps.length];

        // move the robot and generate probability distribution by forward algorithm.
        for (int i = 0; i < steps.length; i++) {
            if (maze.isLegal(position.getKey() + steps[i][0], position.getValue() + steps[i][1])) {
                position = new Pair<>(position.getKey() + steps[i][0], position.getValue() + steps[i][1]);
            }
            actualPath.add(position);
            sensorReadings[i] = getTileColor(position);
            probabilityDistribution[i + 1] = forward(probabilityDistribution[i], getTileColor(position));
        }

        // get smoothy probability distribution by forward-backward algorithm.
        this.smoothyDistribution = forwardBackward(firstDistribution, sensorReadings);
        // get most likely path using smoothy distribution:
        this.likelyPath = getMostLikelyPath(this.smoothyDistribution);
        int[] path = viterbi(firstDistribution, sensorReadings);
        this.viterbiPath.add(index.get(1));
        for (int aPath : path) {
            this.viterbiPath.add(index.get(aPath));
        }
    }

    /**
     * The report contains:
     * 1. the maze
     * 2. the motions robot taken.
     * 3. the actual path
     * 4. the positions with the largest probability from the distribution at each time step
     * 5. the Viterbi path
     * 6. filtering distribution
     * 7. forward-backward algorithm's distribution
     *
     * @param motions
     */
    public void printReport(String motions) {
        System.out.println("the maze this test used :");
        System.out.println(maze);
        System.out.println("the motions robot taken :\n" + motions);
        System.out.println("the actual path robot move is:");
        for (Pair<Integer, Integer> p : actualPath) {
            System.out.print("(" + p.getKey() + "," + p.getValue() + ")");
        }
        System.out.println("\nthe most likely point chose from smoothy distribution is :");
        for (Pair<Integer, Integer> p : likelyPath) {
            System.out.print("(" + p.getKey() + "," + p.getValue() + ")");
        }
        System.out.println("\nthe viterbi path is:");
        for (Pair<Integer, Integer> p : viterbiPath) {
            System.out.print("(" + p.getKey() + "," + p.getValue() + ")");
        }
        System.out.println("\n\nthe smoothy distribution is:");
        printDistribution(this.smoothyDistribution);
        System.out.println("\nthe filter distribution is:");
        printDistribution(this.probabilityDistribution);
    }

    private void printDistribution(double[][] distribution) {
        for (int i = 0; i < distribution[0].length; i++) {
            System.out.print("_____________");
        }
        System.out.println();
        System.out.printf("| steps |");
        for (int i = 1; i <= distribution[0].length; i++) {
            System.out.printf("   (%d,%d)   |", index.get(i).getKey(), index.get(i).getValue());
        }
        System.out.println();
        for (int i = 0; i < distribution.length; i++) {
            System.out.printf("| step%d | ", i);
            for (int j = 0; j < distribution[0].length; j++) {
                System.out.printf("%.7f | ", distribution[i][j]);
            }
            System.out.println();
        }
    }

    /**
     * Choose the largest probability from the distribution at each time step
     *
     * @param distribution the probability distribution
     * @return the list of positions with the largest probability.
     */
    private List<Pair<Integer, Integer>> getMostLikelyPath(double[][] distribution) {
        List<Pair<Integer, Integer>> path = new ArrayList<>();

        for (int i = 0; i < distribution.length; i++) {
            int maxIndex = 0;
            double maxValue = 0;
            for (int j = 0; j < distribution[0].length; j++) {
                if (distribution[i][j] > maxValue) {
                    maxValue = distribution[i][j];
                    maxIndex = j;
                }
            }
            path.add(this.index.get(maxIndex + 1));
        }

        return path;
    }

    /**
     * the forward-backward algorithm described in the book.
     *
     * @param prior          the prior probability
     * @param sensorReadings the color sequence given by sensor.
     * @return the distribution
     */
    private double[][] forwardBackward(double[] prior, char[] sensorReadings) {
        double[][] forwardDistribution = new double[sensorReadings.length + 1][];
        double[][] smoothDistribution = new double[sensorReadings.length + 1][];
        double[] backwardMessage = new double[prior.length];

        //initial forward msg and backward msg:
        forwardDistribution[0] = prior;
        for (int i = 0; i < backwardMessage.length; i++) {
            backwardMessage[i] = 1;
        }

        for (int i = 0; i < sensorReadings.length; i++) {
            forwardDistribution[i + 1] = forward(forwardDistribution[i], sensorReadings[i]);
        }

        for (int i = sensorReadings.length; i > 0; i--) {
            smoothDistribution[i] = MatrixTools.vectorNormalize(MatrixTools.vectorMultiple(forwardDistribution[i], backwardMessage));
            backwardMessage = backward(backwardMessage, sensorReadings[i - 1]);
        }
        smoothDistribution[0] = new double[prior.length];
        return smoothDistribution;
    }

    /**
     * the filter algorithm looking forward from the first position.
     *
     * @param lastOne the probability of the prior position.
     * @param color   the color given by sensor in this state.
     * @return the probability of each state in current step.
     */
    private double[] forward(double[] lastOne, char color) {
        double[][] O = getSensorModelByColor(color);
        double[][] line = new double[1][];
        line[0] = lastOne;
        double[][] result = MatrixTools.multiple(MatrixTools.multiple(O, MatrixTools.transpose(T)), MatrixTools.transpose(line));
        return MatrixTools.transpose(MatrixTools.normalize(result))[0];
    }

    /**
     * @param bv    a representation of the backward message, initially all 1s
     * @param color evidence value.
     * @return
     */
    private double[] backward(double[] bv, char color) {
        double[][] O = getSensorModelByColor(color);
        double[][] line = new double[1][];
        line[0] = bv;
        double[][] result = MatrixTools.multiple(MatrixTools.multiple(T, O), MatrixTools.transpose(line));
        return MatrixTools.transpose(result)[0];
    }

    /**
     * calculate the transition probability from p1 to p2.
     *
     * @param p1 position 1
     * @param p2 position 2
     * @return probability
     */
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

        if (Math.abs(x2 - x1) + Math.abs(y2 - y1) == 1) return 0.25;
        if (p1.equals(p2)) return walls / 4.0;
        return 0;
    }

    /**
     * calculate the sensor probability
     *
     * @param position
     * @param color
     * @return
     */
    private double getSensorProbability(Pair<Integer, Integer> position, char color) {
        int x = position.getKey();
        int y = position.getValue();
        char trueColor = maze.getChar(x, y);
        if (trueColor == color) {
            return 0.88;
        }
        return 0.04;
    }

    /**
     * simulate the robot's sensor, give the true color 88%, other color each 4%
     *
     * @param position the position robot on
     * @return color
     */
    private char getTileColor(Pair<Integer, Integer> position) {
        List<Character> colors = new ArrayList<>();
        colors.add('r');
        colors.add('g');
        colors.add('b');
        colors.add('y');
        int rand = new Random().nextInt(100);
        char trueColor = maze.getChar(position.getKey(), position.getValue());
        int i = colors.indexOf(trueColor);
        if (rand > 87 && rand < 92) {
            i = (i + 1) % 4;
        } else if (rand > 91 && rand < 96) {
            i = (i + 2) % 4;
        } else if (rand > 95 && rand < 100) {
            i = (i + 3) % 4;
        }
        return colors.get(i);
    }

    private double[][] getSensorModelByColor(char color) {
        switch (color) {
            case 'r':
                return Or;
            case 'g':
                return Og;
            case 'b':
                return Ob;
            case 'y':
                return Oy;
            default:
                throw new RuntimeException("wrong color");
        }
    }

    /**
     * viterbi algorithm described in the book.
     *
     * @param prior          the initial probability vector.
     * @param sensorReadings the color sequence read from sensor.
     * @return the most likely path.
     */
    private int[] viterbi(double[] prior, char[] sensorReadings) {
        double[][] maxProb = new double[tileNumber][sensorReadings.length];
        int[][] maxArg = new int[tileNumber][sensorReadings.length];

        for (int i = 0; i < prior.length; i++) {
            maxProb[i][0] = prior[i] * getSensorModelByColor(sensorReadings[0])[i][i];
            maxArg[i][0] = 0;
        }

        for (int i = 1; i < sensorReadings.length; i++) {
            for (int j = 0; j < prior.length; j++) {
                double maxValue = 0;
                int maxIndex = 0;
                for (int k = 0; k < prior.length; k++) {
                    double temp = maxProb[k][i - 1] * T[k][j] * getSensorModelByColor(sensorReadings[i])[j][j];
                    if (temp > maxValue) {
                        maxValue = temp;
                        maxIndex = k;
                    }
                }

                maxProb[j][i] = maxValue;
                maxArg[j][i] = maxIndex;
            }
        }

        int[] result = new int[sensorReadings.length];
        double maxValue = 0;
        int maxIndex = 0;
        for (int i = 0; i < prior.length; i++) {
            if (maxProb[i][sensorReadings.length - 1] > maxValue) {
                maxValue = maxProb[i][sensorReadings.length - 1];
                maxIndex = i;
            }
        }
        result[result.length - 1] = maxIndex + 1;
        for (int i = result.length - 1; i > 0; i--) {
            result[i - 1] = maxArg[result[i] - 1][i] + 1;
        }

        return result;
    }

    public static void main(String[] args) {
        ProbabilisticProblem probabilisticProblem = new ProbabilisticProblem();
        int[][] steps = {Maze.EAST, Maze.SOUTH, Maze.WEST, Maze.WEST, Maze.NORTH, Maze.EAST, Maze.SOUTH, Maze.WEST, Maze.WEST, Maze.NORTH};
        probabilisticProblem.doMoves(steps);
        probabilisticProblem.printReport("eswwneswwn");
    }
}
