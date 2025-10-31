package spacegraph;

import jcog.Util;
import jcog.activation.SigmoidActivation;
import jcog.nndepr.EvolvingGraphNN;
import jcog.table.ARFF;
import jcog.table.DataTable;
import jcog.tensor.Tensor;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.layout.Force2D;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class EvolvingGraphNNDemo {
    public static void main(String[] args) throws ARFF.ARFFParseError, IOException {
        var data = new ARFF(irisARFF);
        int is = 0, ie = 4, os = 4, oe = 7;

        var n = new EvolvingGraphNN();
        n.activationDefault = SigmoidActivation.the;
        n.addInputs(ie-is);
        n.addOutputs(oe-os);
        n.addLayersConnectFull(6,6,4);

        var g = new Graph2D<EvolvingGraphNN.Node>()
                .update(
                    new Force2D<>()
                    //new ForceAtlas2D<>()
                    //new FruchtermanReingold2D<>()
                )
                .render((node, graph) -> {
                    float speed = 4;
                    if (node.id.isInput())
                        node.move(0, +speed);
                    else if (node.id.isOutput())
                        node.move(0, -speed);

                    //node.pri = (float) node.id.importance();

                    node.id.outs.forEach(e -> {
                        var edge = graph.edge(node, graph.nodeOrAdd(e.to));
                        double w = e.weight;
                        float edgeMin = 0.1f;

                        //float edgeMax = 0.02f;
                        //edge.weight = edgeMin + (float) (edgeMax * Util.abs(w));
                        edge.weight = edgeMin;

                        float red;
                        red = w > 0 ? (float) Math.min(1, w) : 0;
                        float green;
                        green = w < 0 ? (float) Math.min(1, -w) : 0;
                        edge.color(red, green, edgeMin);
                    });
                });

        SpaceGraph.window(g.widget(), 1000, 1000);

        double learningRate =
                0.005f;

        int iters = 0, maxIters = 16*4096;


        int cases = data.rowCount();
        int batchSize = 4;

        double lossSum, lossMean;
        do {
            lossSum = 0;
            for (int b = 0; b < batchSize; b++) {
                var row = data.row(ThreadLocalRandom.current().nextInt(cases));
                double[] x = DataTable.toDouble(row, is, ie),
                        y = DataTable.toDouble(row, os, oe);


                double[] yPred = n.put(x, y, learningRate);
                double[] delta = Util.sub(y, yPred);
                //System.out.println(n2(x) + "\t" + n2(y) + "\t" + n2(delta));
                double loss = Tensor.row(y).mse(Tensor.row(yPred)).scalar();
                lossSum += loss;

            }
            lossMean = lossSum / batchSize;

            g.set(n.nodes());


            Util.sleepMS(1);
            System.out.println(lossMean);


            {
                double rate = lossMean;

//                //weight decay
//                n.edges.forEach(e -> e.weight *= 0.9999);

                float growNodes = n.hiddens.size() < (n.inputCount() + n.outputCount()) ? 2 : 1;

                float mutRate = (float) (0.001f * rate);

                if (n.random.nextFloat() < mutRate * growNodes) {
//                var in = N.randomNode(true, true, false);
//                var out = N.randomNode(false, true, true);
//                if (in!=out) {
//                    var h = N.newHiddenNode();
//                    N.addEdge(in, h);
//                    N.addEdge(h, out);
//                }

                    var in1 = n.randomNode(true, true, false);
                    var in2 = n.randomNode(true, true, false);
                    if (in1 != in2) {
                        var out = n.randomNode(false, true, true);
                        if (in1 != out) {
                            var h = n.newHiddenNode();
                            n.addEdge(in1, h);
                            n.addEdge(in2, h);
                            n.addEdge(h, out);
                        }
                    }

                } else if (n.random.nextFloat() < mutRate) {
                    n.removeWeakestEdge();
//            } else if (N.random.nextFloat() < mutRate*1) {
//                N.removeWeakestNode();
                }
            }


        } while (lossMean > 0.0001 && iters++ < maxIters);
    }


    private static final String irisARFF = """
            % 1. Title: Iris Plants Database
            %\s
            % 2. Sources:
            %      (a) Creator: R.A. Fisher
            %      (b) Donor: Michael Marshall (MARSHALL%PLU@io.arc.nasa.gov)
            %      (c) Date: July, 1988
            %\s
            % 3. Past Usage:
            %    - Publications: too many to mention!!!  Here are a few.
            %    1. Fisher,R.A. "The use of multiple measurements in taxonomic problems"
            %       Annual Eugenics, 7, Part II, 179-188 (1936); also in "Contributions
            %       to Mathematical Statistics" (John Wiley, NY, 1950).
            %    2. Duda,R.O., & Hart,P.E. (1973) Pattern Classification and Scene Analysis.
            %       (Q327.D83) John Wiley & Sons.  ISBN 0-471-22361-1.  See page 218.
            %    3. Dasarathy, B.V. (1980) "Nosing Around the Neighborhood: A New System
            %       Structure and Classification Rule for Recognition in Partially Exposed
            %       Environments".  IEEE Transactions on Pattern Analysis and Machine
            %       Intelligence, Vol. PAMI-2, No. 1, 67-71.
            %       -- Results:
            %          -- very low misclassification rates (0% for the setosa class)
            %    4. Gates, G.W. (1972) "The Reduced Nearest Neighbor Rule".  IEEE\s
            %       Transactions on Information Theory, May 1972, 431-433.
            %       -- Results:
            %          -- very low misclassification rates again
            %    5. See also: 1988 MLC Proceedings, 54-64.  Cheeseman et al's AUTOCLASS II
            %       conceptual clustering system finds 3 classes in the data.
            %\s
            % 4. Relevant Information:
            %    --- This is perhaps the best known database to be found in the pattern
            %        recognition literature.  Fisher's paper is a classic in the field
            %        and is referenced frequently to this day.  (See Duda & Hart, for
            %        example.)  The data setAt contains 3 classes of 50 instances each,
            %        where each class refers to a type of iris plant.  One class is
            %        linearly separable from the other 2; the latter are NOT linearly
            %        separable from each other.
            %    --- Predicted attribute: class of iris plant.
            %    --- This is an exceedingly simple domain.
            %\s
            % 5. Number of Instances: 150 (50 in each of three classes)
            %\s
            % 6. Number of Attributes: 4 numeric, predictive attributes and the class
            %\s
            % 7. Attribute Information:
            %    1. sepal length in cm
            %    2. sepal width in cm
            %    3. petal length in cm
            %    4. petal width in cm
            %    5. class:\s
            %       -- Iris Setosa
            %       -- Iris Versicolour
            %       -- Iris Virginica
            %\s
            % 8. Missing Attribute Values: None
            %\s
            % Summary Statistics:
            %  \t           Min  Max   Mean    SD   Class Correlation
            %    sepal length: 4.3  7.9   5.84  0.83    0.7826  \s
            %     sepal width: 2.0  4.4   3.05  0.43   -0.4194
            %    petal length: 1.0  6.9   3.76  1.76    0.9490  (high!)
            %     petal width: 0.1  2.5   1.20  0.76    0.9565  (high!)
            %\s
            % 9. Class Distribution: 33.3% for each of 3 classes.

            @RELATION iris

            @ATTRIBUTE "(sepal,length)"\tREAL
            @ATTRIBUTE "(sepal,width)"\tREAL
            @ATTRIBUTE "(petal,length)"\tREAL
            @ATTRIBUTE "(petal,width)"\tREAL
            @ATTRIBUTE "(a)"\tREAL
            @ATTRIBUTE "(b)"\tREAL
            @ATTRIBUTE "(c)"\tREAL

            @DATA
            5.1,3.5,1.4,0.2,1,0,0
            4.9,3.0,1.4,0.2,1,0,0
            4.7,3.2,1.3,0.2,1,0,0
            4.6,3.1,1.5,0.2,1,0,0
            5.0,3.6,1.4,0.2,1,0,0
            5.4,3.9,1.7,0.4,1,0,0
            4.6,3.4,1.4,0.3,1,0,0
            5.0,3.4,1.5,0.2,1,0,0
            4.4,2.9,1.4,0.2,1,0,0
            4.9,3.1,1.5,0.1,1,0,0
            5.4,3.7,1.5,0.2,1,0,0
            4.8,3.4,1.6,0.2,1,0,0
            4.8,3.0,1.4,0.1,1,0,0
            4.3,3.0,1.1,0.1,1,0,0
            5.8,4.0,1.2,0.2,1,0,0
            5.7,4.4,1.5,0.4,1,0,0
            5.4,3.9,1.3,0.4,1,0,0
            5.1,3.5,1.4,0.3,1,0,0
            5.7,3.8,1.7,0.3,1,0,0
            5.1,3.8,1.5,0.3,1,0,0
            5.4,3.4,1.7,0.2,1,0,0
            5.1,3.7,1.5,0.4,1,0,0
            4.6,3.6,1.0,0.2,1,0,0
            5.1,3.3,1.7,0.5,1,0,0
            4.8,3.4,1.9,0.2,1,0,0
            5.0,3.0,1.6,0.2,1,0,0
            5.0,3.4,1.6,0.4,1,0,0
            5.2,3.5,1.5,0.2,1,0,0
            5.2,3.4,1.4,0.2,1,0,0
            4.7,3.2,1.6,0.2,1,0,0
            4.8,3.1,1.6,0.2,1,0,0
            5.4,3.4,1.5,0.4,1,0,0
            5.2,4.1,1.5,0.1,1,0,0
            5.5,4.2,1.4,0.2,1,0,0
            4.9,3.1,1.5,0.1,1,0,0
            5.0,3.2,1.2,0.2,1,0,0
            5.5,3.5,1.3,0.2,1,0,0
            4.9,3.1,1.5,0.1,1,0,0
            4.4,3.0,1.3,0.2,1,0,0
            5.1,3.4,1.5,0.2,1,0,0
            5.0,3.5,1.3,0.3,1,0,0
            4.5,2.3,1.3,0.3,1,0,0
            4.4,3.2,1.3,0.2,1,0,0
            5.0,3.5,1.6,0.6,1,0,0
            5.1,3.8,1.9,0.4,1,0,0
            4.8,3.0,1.4,0.3,1,0,0
            5.1,3.8,1.6,0.2,1,0,0
            4.6,3.2,1.4,0.2,1,0,0
            5.3,3.7,1.5,0.2,1,0,0
            5.0,3.3,1.4,0.2,1,0,0
            7.0,3.2,4.7,1.4,0,1,0
            6.4,3.2,4.5,1.5,0,1,0
            6.9,3.1,4.9,1.5,0,1,0
            5.5,2.3,4.0,1.3,0,1,0
            6.5,2.8,4.6,1.5,0,1,0
            5.7,2.8,4.5,1.3,0,1,0
            6.3,3.3,4.7,1.6,0,1,0
            4.9,2.4,3.3,1.0,0,1,0
            6.6,2.9,4.6,1.3,0,1,0
            5.2,2.7,3.9,1.4,0,1,0
            5.0,2.0,3.5,1.0,0,1,0
            5.9,3.0,4.2,1.5,0,1,0
            6.0,2.2,4.0,1.0,0,1,0
            6.1,2.9,4.7,1.4,0,1,0
            5.6,2.9,3.6,1.3,0,1,0
            6.7,3.1,4.4,1.4,0,1,0
            5.6,3.0,4.5,1.5,0,1,0
            5.8,2.7,4.1,1.0,0,1,0
            6.2,2.2,4.5,1.5,0,1,0
            5.6,2.5,3.9,1.1,0,1,0
            5.9,3.2,4.8,1.8,0,1,0
            6.1,2.8,4.0,1.3,0,1,0
            6.3,2.5,4.9,1.5,0,1,0
            6.1,2.8,4.7,1.2,0,1,0
            6.4,2.9,4.3,1.3,0,1,0
            6.6,3.0,4.4,1.4,0,1,0
            6.8,2.8,4.8,1.4,0,1,0
            6.7,3.0,5.0,1.7,0,1,0
            6.0,2.9,4.5,1.5,0,1,0
            5.7,2.6,3.5,1.0,0,1,0
            5.5,2.4,3.8,1.1,0,1,0
            5.5,2.4,3.7,1.0,0,1,0
            5.8,2.7,3.9,1.2,0,1,0
            6.0,2.7,5.1,1.6,0,1,0
            5.4,3.0,4.5,1.5,0,1,0
            6.0,3.4,4.5,1.6,0,1,0
            6.7,3.1,4.7,1.5,0,1,0
            6.3,2.3,4.4,1.3,0,1,0
            5.6,3.0,4.1,1.3,0,1,0
            5.5,2.5,4.0,1.3,0,1,0
            5.5,2.6,4.4,1.2,0,1,0
            6.1,3.0,4.6,1.4,0,1,0
            5.8,2.6,4.0,1.2,0,1,0
            5.0,2.3,3.3,1.0,0,1,0
            5.6,2.7,4.2,1.3,0,1,0
            5.7,3.0,4.2,1.2,0,1,0
            5.7,2.9,4.2,1.3,0,1,0
            6.2,2.9,4.3,1.3,0,1,0
            5.1,2.5,3.0,1.1,0,1,0
            5.7,2.8,4.1,1.3,0,1,0
            6.3,3.3,6.0,2.5,0,0,1
            5.8,2.7,5.1,1.9,0,0,1
            7.1,3.0,5.9,2.1,0,0,1
            6.3,2.9,5.6,1.8,0,0,1
            6.5,3.0,5.8,2.2,0,0,1
            7.6,3.0,6.6,2.1,0,0,1
            4.9,2.5,4.5,1.7,0,0,1
            7.3,2.9,6.3,1.8,0,0,1
            6.7,2.5,5.8,1.8,0,0,1
            7.2,3.6,6.1,2.5,0,0,1
            6.5,3.2,5.1,2.0,0,0,1
            6.4,2.7,5.3,1.9,0,0,1
            6.8,3.0,5.5,2.1,0,0,1
            5.7,2.5,5.0,2.0,0,0,1
            5.8,2.8,5.1,2.4,0,0,1
            6.4,3.2,5.3,2.3,0,0,1
            6.5,3.0,5.5,1.8,0,0,1
            7.7,3.8,6.7,2.2,0,0,1
            7.7,2.6,6.9,2.3,0,0,1
            6.0,2.2,5.0,1.5,0,0,1
            6.9,3.2,5.7,2.3,0,0,1
            5.6,2.8,4.9,2.0,0,0,1
            7.7,2.8,6.7,2.0,0,0,1
            6.3,2.7,4.9,1.8,0,0,1
            6.7,3.3,5.7,2.1,0,0,1
            7.2,3.2,6.0,1.8,0,0,1
            6.2,2.8,4.8,1.8,0,0,1
            6.1,3.0,4.9,1.8,0,0,1
            6.4,2.8,5.6,2.1,0,0,1
            7.2,3.0,5.8,1.6,0,0,1
            7.4,2.8,6.1,1.9,0,0,1
            7.9,3.8,6.4,2.0,0,0,1
            6.4,2.8,5.6,2.2,0,0,1
            6.3,2.8,5.1,1.5,0,0,1
            6.1,2.6,5.6,1.4,0,0,1
            7.7,3.0,6.1,2.3,0,0,1
            6.3,3.4,5.6,2.4,0,0,1
            6.4,3.1,5.5,1.8,0,0,1
            6.0,3.0,4.8,1.8,0,0,1
            6.9,3.1,5.4,2.1,0,0,1
            6.7,3.1,5.6,2.4,0,0,1
            6.9,3.1,5.1,2.3,0,0,1
            5.8,2.7,5.1,1.9,0,0,1
            6.8,3.2,5.9,2.3,0,0,1
            6.7,3.3,5.7,2.5,0,0,1
            6.7,3.0,5.2,2.3,0,0,1
            6.3,2.5,5.0,1.9,0,0,1
            6.5,3.0,5.2,2.0,0,0,1
            6.2,3.4,5.4,2.3,0,0,1
            5.9,3.0,5.1,1.8,0,0,1
            """;

}
