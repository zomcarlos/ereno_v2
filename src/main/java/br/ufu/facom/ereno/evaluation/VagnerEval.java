package br.ufu.facom.ereno.evaluation;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.trees.RandomTree;
import weka.classifiers.trees.REPTree;
import weka.classifiers.Evaluation;

public class VagnerEval {
    public static void main(String[] args) {
        try {
            String targetAttack;
            if (args.length < 3) {
                System.out.println("Uso: java -jar ERENO.jar <trainPath> <testPath> <classifier> <attack (optional)> ");
                System.out.println("Classificadores suportados: j48, naivebayes, randomforest, randomtree, reptree");
                return;
            } else if (args.length == 3) {
                targetAttack = "full";
            } else {
                targetAttack = args[3];
            }

            // Caminhos para os datasets
            String trainPath = args[0];
            String testPath = args[1];
            String classifierType = args[2].toLowerCase();

            // Carregar os datasets de treino e teste
            System.out.println("Carregando datasets...");
            DataSource trainSource = new DataSource(trainPath);
            Instances trainData = trainSource.getDataSet();

            DataSource testSource = new DataSource(testPath);
            Instances testData = testSource.getDataSet();

            // Definir o atributo de classe (última coluna)
            if (trainData.classIndex() == -1) {
                trainData.setClassIndex(trainData.numAttributes() - 1);
            }
            if (testData.classIndex() == -1) {
                testData.setClassIndex(testData.numAttributes() - 1);
            }

            // Filtrar instâncias
            System.out.println("Filtrando instâncias...");
            trainData = filterInstances(trainData, targetAttack);
            testData = filterInstances(testData, targetAttack);

            // Selecionar o classificador com base no terceiro parâmetro
            System.out.println("Selecionando classificador...");
            Classifier classifier;

            switch (classifierType) {
                case "j48":
                    classifier = new J48();
                    ((J48) classifier).setUnpruned(true); // Configuração para árvore não podada
                    break;
                case "naivebayes":
                    classifier = new NaiveBayes();
                    break;
                case "randomforest":
                    classifier = new RandomForest();
                    break;
                case "randomtree":
                    classifier = new RandomTree();
                    break;
                case "reptree":
                    classifier = new REPTree();
                    break;
                default:
                    System.out.println("Classificador desconhecido: " + classifierType);
                    System.out.println("Classificadores suportados: j48, naivebayes, randomforest, randomtree, reptree");
                    return;
            }

            // Treinar o classificador
            System.out.println("Treinando o modelo...");
            classifier.buildClassifier(trainData);

            // Avaliar o modelo com os dados de teste
            System.out.println("Testando o modelo...");
            Evaluation eval = new Evaluation(trainData);
            eval.evaluateModel(classifier, testData);

            // Exibir resultados do teste
            System.out.println("\n=== Resultados da Avaliação ===");
            System.out.println(eval.toSummaryString("\nResumo:\n", false));
            System.out.println("Matriz de Confusão:\n" + eval.toMatrixString());

            if (classifier instanceof J48) {
                System.out.println("\nÁrvore de Decisão Gerada:\n" + classifier);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Instances filterInstances(Instances data, String targetAttack) throws Exception {
        Instances filteredData = new Instances(data, 0);

        if (targetAttack.equals("full")) {
            return data;
        }


        int qtdNormal = 0;
        int qtdAtaque = 0;
        for (int i = 0; i < data.numInstances(); i++) {
            String classValue = data.instance(i).stringValue(data.classIndex()).toLowerCase();

            // Manter apenas instâncias da classe "normal" ou do ataque especificado
            if (classValue.equals("normal")) {
                qtdNormal = qtdNormal + 1;
                filteredData.add(data.instance(i));
            } else if (classValue.equals(targetAttack.toLowerCase())) {
                filteredData.add(data.instance(i));
                qtdAtaque = qtdAtaque + 1;
            }
        }

        System.out.println("Instâncias mantidas: " + filteredData.numInstances() + "(Normal: " + qtdNormal + " / Ataque: )" + qtdAtaque);
        return filteredData;
    }
}
