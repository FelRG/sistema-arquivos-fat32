import edu.so.Diretorio;
import edu.so.Disco;
import edu.so.Fat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        try {

            Disco disco = new Disco();
            int[] fatInt = new int[16 * 1024];

            //busca a fat e ecoloca em um array de inteiro
            byte[] fatByte = disco.leBloco(1);
            ByteBuffer bb   = ByteBuffer.wrap(fatByte);
            for (int i = 0; i < 16*1024; i++) {
                fatInt[i] = bb.getInt();
            }

            //jogando a  fat buscada para um objeto fat
            Fat fat = new Fat(fatInt, disco);



            Diretorio diretorio = new Diretorio();
            diretorio.limparDiretorio(disco);


            int opcao = -1;
            do{

                System.out.println("\nDigite um numero para sinalizar o que deseja fazer:\n1 - Criar Arquivo\n2 - Ler Arquivo\n3 - Excluir Arquivo\n4 - Extender Arquivo\n5 - Ver Espaco Livre\n6 - Mostrar Arquivos Existentes\n0 - Sair\nAVISO: Voce pode criar um arquivo com dados existentes em um arquivo do seu explorador de arquivos que esta na pasta do programa (funcionou com pdf e png tambem) ou pode criar um arquivo digitando dados no terminar");
                Scanner scanner = new Scanner(System.in);
                opcao = scanner.nextInt();

                switch (opcao) {
                    case 1:

                        System.out.println("Para criar com um arquivo jah existente precione 1\nPara criar digitando os dados precione 2:");
                        int umDois = scanner.nextInt();
                        if(umDois == 1){

                            System.out.println("Digite o nome do arquivo existente na pasta (com extensao)");
                            String fileName = scanner.next();
                            File file = new File(fileName);
                            byte[] fileContent = null;

                            try (FileInputStream fis = new FileInputStream(file)) {
                                fileContent = new byte[(int) file.length()];
                                int bytesRead = fis.read(fileContent);

                                if (bytesRead != fileContent.length) {
                                    System.out.println("Erro ao ler o arquivo completamente.");

                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            System.out.println("Agora digite como sera o nome do arquivo salvo no disco (com extensao)");
                            String nomeArquivo = scanner.next();
                            fat.create(nomeArquivo, fileContent);

                        } else if (umDois == 2) {

                            System.out.println("Digite o nome do arquivo que sera criado (com extensao):");
                            String n = scanner.next();
                            System.out.println("Agora digite o conteudo:");
                            String c = scanner.next();
                            fat.create(n, c.getBytes(StandardCharsets.UTF_8));

                        } else {
                            System.out.println("Invalido");
                        }

                        break;
                        case 2:

                            System.out.println("Digite o arquivo que deseja ler (sem extensao):");
                            String nomeArquivo = scanner.next();
                            fat.read(nomeArquivo);

                            break;
                            case 3:

                                System.out.printf("Digite o nome do arquivo que sera excluido (sem extensao):");
                                nomeArquivo = scanner.next();
                                fat.remove(nomeArquivo);
                                break;
                                case 4:

                                    System.out.println("Qual arquivo deseja extender?");
                                    String nome = scanner.next();
                                    System.out.println("Digite o que serah extendido:");
                                    String extensao = scanner.next();
                                    byte[] extensaoBytes = extensao.getBytes(StandardCharsets.UTF_8);
                                    fat.append(nome, extensaoBytes);

                                    break;
                                    case 5:
                                        System.out.println("Espaco disponivel no disco: " + fat.calculaEspacoLivre() +"B");
                                        System.out.println("Pause! Precione qualquer letra e de enter para continuar");
                                        String pausa = scanner.next();
                                        break;
                                        case 6:

                                            try {
                                                Diretorio dd = new Diretorio();
                                                System.out.println(dd.deserializeDiretorios(disco));
                                                System.out.println("Pause! Precione qualquer letra e de enter para continuar");
                                                String p = scanner.next();
                                            } catch (Exception e){

                                            }

                                            break;
                                            case 0:
                                                break;
                    default:
                        System.out.println("Invalido");
                        return;
                }

            } while(opcao != 0);





        } catch (Exception e) {

        }





    }
}
