package edu.so;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Fat implements FileSystem{

    ArrayList<Diretorio> arquivos = new ArrayList<>(16 * 1024);
    int[] fat = new int[16 * 1024];
    Disco disco;

    public Fat(int[] fat, Disco disco) {
        this.fat = fat;
        this.disco = disco;
    }

    public void create(String nomeArquivo, byte[] dados){

        //calcular espaco livre
        int espacoDados = dados.length;

        if(espacoDados > calculaEspacoLivre()) {
            System.out.println("Espaço insuficiente");
        }

        System.out.println("\n\n\n\n vai cadastrar");

        //tem espaco para armazenar, entao bora criar o diretorio/arquivo

        String[] parts = nomeArquivo.split("\\.", 2);
        String nome = "";
        String extensao = "";
        // Verifica se a divisão foi bem-sucedida
        if (parts.length == 2) {
            nome = parts[0];
            extensao = parts[1];
        }

        //ler fat para buscar primeiro espaco vago
        leFat();

        int primeiroEspaco = 0;
        for(int i = 2; i < 16 * 1024; i++){
            if(fat[i] == -1){
                primeiroEspaco = i;
                System.out.println("iiiii: " + i);
                break;
            }
        }

        // criando a representacao do arquivo e adicionando na lista de representacoes de arquivos
        //ANTES PREECISO LER O DIRETORIO, PARA ADICIONAR O NOVO ARQUIVO NO FINAL DA LISTA DE ARQUIVOS JA EXISTENTES NO DISCO
        Diretorio arquivo = new Diretorio(nome, extensao, espacoDados, primeiroEspaco);

        //caso nao tenha nenhum arquivo nao precisa ler o diretorio, so inseri-lo
        if(this.fat[0] == -1){

                    this.arquivos.add(arquivo);

            try {
                arquivo.serializeDiretorios(this.arquivos, this.disco);
            } catch (Exception e){

            }

            this.fat[0] = 0;

        } else {
            //caso ja tenha arquivos primeiro eh necessario ler e manipula-lo (achar espaco livre para ocupar)
            try{


                ArrayList<Diretorio> d = arquivo.deserializeDiretorios(disco);

                d.add(arquivo);



                arquivo.serializeDiretorios(d, this.disco);

            } catch (Exception e){

            }

        }



        //necessario enviar para o disco

        //preciso quebrar o dado em blocos de 64kb

        final int BLOCK_SIZE = 64 * 1024; // 64 KB = 65536 bytes



        // Dividindo o array original em blocos menores ________________________
        int aux = -1;
        int start = 0;
        while (start < dados.length) {
            int end = Math.min(dados.length, start + BLOCK_SIZE);
            byte[] block = new byte[end - start];
            System.arraycopy(dados, start, block, 0, block.length);

            System.out.println("bloc" + block);
            //agora devo armazenar cada bloco no disco
            for(int i = primeiroEspaco; i < 16 * 1024; i++){

                if(this.fat[i] == -1){
                    //primeiro altera na fat que a posicao estah sendo usada
                    this.fat[i] = 0;

                    //caso aux for diferente de -1 (numero inicializado) quer dizer que precisa alterar o valor da fat no indice anterior (aux)
                    if(aux != -1){
                        this.fat[aux] = i;
                    }
                    aux = i;

                    try {
                        this.disco.escreveBloco(i, block);

                        break;
                    } catch (Exception e){

                    }

                }

            }



            start += BLOCK_SIZE;
        }


    //___________________________________
        //escrevendo a fat no disco
        try {
            escreveFat();
        } catch (Exception e){

        }


        System.out.println("\n\nfat apos create:\n\n");
        for(int i = 0; i < 16 * 1024; i++){
            System.out.println(this.fat[i]);
        }


        try {
            ArrayList<Diretorio> dir = arquivo.deserializeDiretorios(disco);
            System.out.println(dir.size());
            System.out.println(dir);
        } catch (Exception e){

        }

        System.out.println("ARQUIVO CRIADO COM SUCESSO! Acima está todas as posicoes da fat\nPara chegar ao inicio dela, aperte Ctrl + F e pesquise por 'fat apos create:'. Acima tambem estao todos os arquivos existentes");
        Scanner input = new Scanner(System.in);
        //apenas para dar um pause
        System.out.println("Pause! Precione qualquer letra e de enter para continuar");
        String pausa = input.next();








    }

    public int calculaEspacoLivre(){
        int cont = 0;
        for(int i = 2; i < 16 * 1024; i++){
            if(this.fat[i] == -1){
                cont++;
            }
        }
        System.out.println("contador  " + cont);
        return cont * 64 * 1024;
    }

    public void leFat(){

        try {

            byte[] fatByte = this.disco.leBloco(1);
            ByteBuffer bb   = ByteBuffer.wrap(fatByte);
            for (int i = 0; i < 16*1024; i++) {
                this.fat[i] = bb.getInt();
            }

        } catch (Exception e){

        }


    }

    private void escreveFat() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(64*1024);
        for (int f : this.fat) {
            bb.putInt(f);
        }
        byte[] blocoFat = bb.array();
        disco.escreveBloco(1, blocoFat);
    }


    public void read(String fileName) {

        //logo ja vou tratar o nome do arquivo que veio, ele deve ter exatamente 8 caracteres para as comparacoes
        if (fileName.length() > 8) {
            fileName = fileName.substring(0, 8);
        } else if (fileName.length() < 8) {
            do {
                fileName += " ";
            } while (fileName.length() < 8);
        }


        ArrayList<Diretorio> arquivos = new ArrayList<>();
        int tamanhoArquivo = 0;
        //buscando bloco 0
        try {

            arquivos = new Diretorio().deserializeDiretorios(this.disco);

        } catch (Exception e) {

        }

        Diretorio representacaoArquivo = null;
        //vendo se existe o arquivo
        int blocoInicial = -1;
        for (Diretorio arquivo : arquivos) {

            if (arquivo.getNomeArquivo().equals(fileName)) {
                System.out.println("jah");
                representacaoArquivo = arquivo;
                //armazenando o bloco inicial para ler
                blocoInicial = arquivo.getBlocoInicial();
                System.out.println("bloco ini:" + blocoInicial);
                tamanhoArquivo = arquivo.getTamanho();
                break;
            }

        }

        //em cada posicao este array vai um bloco do disco, caso arquivo esteja em varios blocos
        ArrayList<byte[]> conteudoTotalDividido = new ArrayList<>();
        if (blocoInicial == -1) {

            System.out.println("arquivo nao existe");

        } else {

            System.out.println("arquivo existe");

            leFat();

            //var axuliar para nao deixar dar loop infinit  no for mais adiante
            Boolean naoEntra = false;
            //vairavel auxiliar para conseguir ler todos os bloco de um arquivo
            int proximoBloco = -1;

            //navegar toda fat
            for (int i = 0; i < 16 * 1024; i++) {

                //achando o bloco inicial vou o ler e guardar no array de partes do arquivo
                if (i == blocoInicial && naoEntra == false) {

                    try {

                        conteudoTotalDividido.add(this.disco.leBloco(i));

                    } catch (Exception e) {

                    }

                    //caso o arquivo so esteja neste bloco jah esta lido tudo
                    if (this.fat[i] == 0) {
                        break;
                    }

                    //guardar nesta variavel, com auxilio da fat, qual o proximo bloco sera lido
                    proximoBloco = this.fat[i];

                    //colocando o i em zero para logica de ler sempre tudo do disco (EX: a segunda parte do arquivo pode estar antes da primeira)
                    i = 0;
                    naoEntra = true;

                } else {
                    //entra aqui quando ja leu o primeiro bloco
                    if (i == proximoBloco) {

                        try {

                            conteudoTotalDividido.add(this.disco.leBloco(i));

                        } catch (Exception e) {

                        }


                        //caso for zero ja leu tudo
                        if (this.fat[i] == 0) {
                            break;
                        } else {
                            proximoBloco = this.fat[i];
                        }

                        //colocando o i em zero para logica de ler sempre tudo do disco (EX: a segunda parte do arquivo pode estar antes da primeira)
                        i = 0;

                    }

                }

            }

            int tamanhoTotal = 0;
            for (byte[] array : conteudoTotalDividido) {
                tamanhoTotal += array.length;
            }

            //aqui vai estar o conteudo com todos os byte[] extraidos do ArrayList de byte[]
            byte[] conteudoTotal = new byte[tamanhoTotal];
            int posicaoAtual = 0;

            // Copiando os bytes de cada array no ArrayList para o array final
            for (byte[] array : conteudoTotalDividido) {
                System.arraycopy(array, 0, conteudoTotal, posicaoAtual, array.length);
                posicaoAtual += array.length;
            }


            byte[] novoArray = new byte[tamanhoArquivo];

            // Copia os dados do array original para o novo array até o tamanho desejado
            System.arraycopy(conteudoTotal, 0, novoArray, 0, tamanhoArquivo);



            String nomeArquivo = representacaoArquivo.getNomeArquivo();
            String extensao = representacaoArquivo.getExtensao();

            String nomeCompletoArquivo = nomeArquivo + "." + extensao;


            try (FileOutputStream fos = new FileOutputStream(nomeCompletoArquivo)) {
                fos.write(novoArray);
                System.out.println("Arquivo escrito com sucesso: " + nomeCompletoArquivo);
            } catch (IOException e) {
                System.err.println("Erro ao escrever o arquivo: " + e.getMessage());
            }



            System.out.println("\n\nDados do arquivo:" + new String(novoArray, StandardCharsets.UTF_8));
            Scanner input = new Scanner(System.in);
            //apenas para dar um pause
            System.out.println("IMPORTANTE: O arquivo solicitado para leitura foi guardado na pasta do programa sistemaArquivos com o nome de " + nomeCompletoArquivo+ " Para abrir de fato o arquivo, procure-o na pasta pelo explorador de arquivos e abra-o");
            System.out.println("Pause! Precione qualquer letra e de enter para continuar");
            String pausa = input.next();


        }






    }



    public void remove(String fileName){

        //logo ja vou tratar o nome do arquivo que veio, ele deve ter exatamente 8 caracteres para as comparacoes
        if (fileName.length() > 8) {
            fileName = fileName.substring(0, 8);
        } else if (fileName.length() < 8) {
            do {
                fileName += " ";
            } while (fileName.length() < 8);
        }




        ArrayList<Diretorio> arquivos = new ArrayList<>();

        //buscando bloco 0 (onde esta o nome do arquivo para depois ver se ele existe)
        try {

            arquivos = new Diretorio().deserializeDiretorios(this.disco);

        } catch (Exception e) {

        }

        //________________________________________________________________
        //vendo se existe o arquivo
        int blocoInicial = -1;


        Boolean deveEntrar = false;
        int tamanhoDiretorio =  arquivos.size();

        //buscando o bloco inicial para fazer a exclusao e jah excluindo a representacao do arquivo
        for (int i = 0; i < arquivos.size(); i++) {

            Diretorio arquivo = arquivos.get(i);

            if (arquivo.getNomeArquivo().equals(fileName) && deveEntrar == false){

                //armazenando o bloco inicial para ler
                blocoInicial = arquivo.getBlocoInicial();

                //cortando fora
                arquivos.remove(i);
                System.out.println("removendo");
                //System.out.println("ioa" + arquivos.get(i));
                deveEntrar = true;

            }

        }


        //atualizando o diretorio no disco
        Diretorio d = new Diretorio();
        try {
            d.serializeDiretorios(arquivos, this.disco);
            System.out.println("gato" + d.deserializeDiretorios(this.disco));
        } catch (Exception e){

        }


            System.out.println("aiq" + arquivos);

        //______________________________________________________________


        if(blocoInicial == -1){
            System.out.println("arquivo nao existe");
        } else {

            System.out.println("Arquivo existe");
            //sempre ler a fat antes
            leFat();

            //colocando -1 em todas as posicoes da fat onde o arquivo ocupava e colocando null no disco ___________________________
            Boolean naoEntra = false;
            int aux = -1;

            for (int i = 0; i < 16 * 1024; i++) {

                if (i == blocoInicial && naoEntra == false) {

                    try {
                        this.disco.escreveBloco(i, null);
                    } catch (Exception e){

                    }

                    if(this.fat[i] == 0){
                        this.fat[i] = -1;
                        break;
                    }


                    aux = this.fat[i];
                    this.fat[i] = -1;
                    naoEntra = true;
                    i = 0;

                } else {

                    if (i == aux) {

                        try {
                            this.disco.escreveBloco(i, null);
                        } catch (Exception e) {

                        }

                        if(this.fat[i] == 0){
                            this.fat[i] = -1;
                            break;
                        }

                        aux = this.fat[i];
                        this.fat[i] = -1;
                        naoEntra = true;
                        i = 0;


                    }
                }

            }
//_______________________________________________________________________

            //escrevendo a fat atualizada no disco
            try {
                escreveFat();
            } catch (Exception e){

            }

            //apenas prints
            System.out.println("\n\nfat apos delete:\n\n");
            for(int i = 0; i < 16 * 1024; i++){
                System.out.println(this.fat[i]);
            }
            System.out.println("ARQUIVO EXCLUIDO COM SUCESSO! Acima está todas as posicoes da fat\nPara chegar ao inicio dela, aperte Ctrl + F e pesquise por 'fat apos delete:'");
            Scanner input = new Scanner(System.in);
            //apenas para dar um pause
            System.out.println("Pause! Precione qualquer letra e de enter para continuar");
            String pausa = input.next();

        }



    }


    public void append(String nomeArquivo, byte[] dados){

        //so vai rodar se tiver espaco
        if(dados.length <= calculaEspacoLivre()){

            //logo ja vou tratar o nome do arquivo que veio, ele deve ter exatamente 8 caracteres para as comparacoes
            if (nomeArquivo.length() > 8) {
                nomeArquivo = nomeArquivo.substring(0, 8);
            } else if (nomeArquivo.length() < 8) {
                do {
                    nomeArquivo += " ";
                } while (nomeArquivo.length() < 8);
            }

            ArrayList<Diretorio> arquivos = new ArrayList<>();

            //buscando o bloco das representacoes de arquivos
            try {

                arquivos = new Diretorio().deserializeDiretorios(this.disco);

            } catch (Exception e) {

            }

            int tamanhoArquivo = 0;
            int blocoInicial = 0;
            boolean teste = false;


            //verificando se o arquivo existe
            for(Diretorio d : arquivos){

                if(d.getNomeArquivo().equals(nomeArquivo)){
                    tamanhoArquivo = d.getTamanho();
                    blocoInicial = d.getBlocoInicial();
                    teste = true;
                    break;
                }

            }

            if(teste == true){

                System.out.println("achou");

                //sempre ler fat
                leFat();


                //essa funcao busca os dados do arquivo (reaproveito codigo da propria funcao de leitura)
                //enquanto busco os dados do arquivo vou atualizando a fat com -1 (como se estivesse apagando mesmo)
                byte[] conteudoExistente = buscaArquivo(blocoInicial);

                byte[] novoArray = new byte[tamanhoArquivo];

                // Copia os dados do array original para o novo array até o tamanho correto para tirar a sujeira no final
                System.arraycopy(conteudoExistente, 0, novoArray, 0, tamanhoArquivo);


                //aqui esta o arquivo com a parte anterior e a nova adicionada
                //IMPORTANTE MUDAR O TAMANHO DO ARQUIVO NO DIRETORIO
                byte[] arrayTudo = new byte[novoArray.length + dados.length];

                for(int i = 0; i < 16 * 1024; i++){

                    Diretorio d = arquivos.get(i);
                    if(d.getNomeArquivo().equals(nomeArquivo)){

                        d.setTamanho(arrayTudo.length);
                        arquivos.set(i, d);
                        break;
                    }

                }

                try{
                    Diretorio d = new Diretorio();
                    d.serializeDiretorios(arquivos, this.disco);
                } catch (Exception e){

                }

                //concatena os dados que ja existiam com os novos
                // Copia os dados do primeiro array para o novo array
                System.arraycopy(novoArray, 0, arrayTudo, 0, novoArray.length);

                // Copia os dados do segundo array para o novo array
                System.arraycopy(dados, 0, arrayTudo, novoArray.length, dados.length);


                //busca o primeiro espaco livre
                int primeiroEspaco = 0;
                for(int i = 2; i < 16 * 1024; i++){
                    if(fat[i] == -1){
                        primeiroEspaco = i;
                        System.out.println("pespaco: " + i);
                        break;
                    }
                }

                //dividir o arrayTudo em blocos de 64KB e escrever no disco e alterar a fat
                final int BLOCK_SIZE = 64 * 1024; // 64 KB = 65536 bytes

                // Dividindo o array original em blocos menores ________________________
                int aux = -1;
                int start = 0;
                while (start < arrayTudo.length) {
                    int end = Math.min(arrayTudo.length, start + BLOCK_SIZE);
                    byte[] block = new byte[end - start];
                    System.arraycopy(arrayTudo, start, block, 0, block.length);

                    System.out.println("bloc" + block);
                    //agora devo armazenar cada bloco no disco
                    for(int i = primeiroEspaco; i < 16 * 1024; i++){

                        if(this.fat[i] == -1){
                            //primeiro altera na fat que a posicao estah sendo usada
                            this.fat[i] = 0;

                            //caso aux for diferente de -1 (numero inicializado) quer dizer que precisa alterar o valor da fat no indice anterior (aux)
                            if(aux != -1){
                                this.fat[aux] = i;
                            }
                            aux = i;

                            try {
                                this.disco.escreveBloco(i, block);

                                break;
                            } catch (Exception e){

                            }

                        }

                    }



                    start += BLOCK_SIZE;
                }
//___________________________________________________________________________
                //atualiza a fat
                try{
                    escreveFat();
                } catch (Exception e){

                }



            } else {
                System.out.println("O arquivo não existe!");
            }

            System.out.println("\n\nfat apos append:\n\n");
            for(int i = 0; i < 16 * 1024; i++){
                System.out.println(this.fat[i]);
            }

            System.out.println("ARQUIVO EXTENDIDO COM SUCESSO! Acima está todas as posicoes da fat\nPara chegar ao inicio dela, aperte Ctrl + F e pesquise por 'fat apos append:'");
            Scanner input = new Scanner(System.in);
            //apenas para dar um pause
            System.out.println("Pause! Precione qualquer letra e de enter para continuar");
            String pausa = input.next();

        } else {
            System.out.println("Espaço insuficiente");
        }




    }

    public byte[] buscaArquivo(int blocoInicial){

        ArrayList<byte[]> conteudoTotalDividido = new ArrayList<>();

        //var axuliar para nao deixar dar loop infinit  no for mais adiante
        Boolean naoEntra = false;
        //vairavel auxiliar para conseguir ler todos os bloco de um arquivo
        int proximoBloco = -1;

        //navegar toda fat
        for (int i = 0; i < 16 * 1024; i++) {

            //achando o bloco inicial vou o ler e guardar no array de partes do arquivo
            if (i == blocoInicial && naoEntra == false) {

                try {

                    conteudoTotalDividido.add(this.disco.leBloco(i));

                } catch (Exception e) {

                }



                //caso o arquivo so esteja neste bloco jah esta lido tudo
                if (this.fat[i] == 0) {
                    this.fat[i] = -1;
                    break;
                }

                //guardar nesta variavel, com auxilio da fat, qual o proximo bloco sera lido
                proximoBloco = this.fat[i];
                this.fat[i] = -1;
                //colocando o i em zero para logica de ler sempre tudo do disco (EX: a segunda parte do arquivo pode estar antes da primeira)
                i = 0;
                naoEntra = true;

            } else {
                //entra aqui quando ja leu o primeiro bloco
                if (i == proximoBloco) {

                    try {

                        conteudoTotalDividido.add(this.disco.leBloco(i));

                    } catch (Exception e) {

                    }


                    //caso for zero ja leu tudo
                    if (this.fat[i] == 0) {
                        this.fat[i] = -1;
                        break;
                    } else {
                        proximoBloco = this.fat[i];
                        this.fat[i] = -1;
                    }

                    //colocando o i em zero para logica de ler sempre tudo do disco (EX: a segunda parte do arquivo pode estar antes da primeira)
                    i = 0;

                }

            }

        }

        try{
            escreveFat();
            leFat();
        } catch (Exception e){

        }

        System.out.println("\n\nalterado\n\n");
        for(int i = 0; i < 16 * 1024; i++){
            System.out.println(" " + this.fat[i]);
        }

        int tamanhoTotal = 0;
        for (byte[] array : conteudoTotalDividido) {
            tamanhoTotal += array.length;
        }

        //aqui vai estar o conteudo com todos os byte[] extraidos do ArrayList de byte[]
        byte[] conteudoTotal = new byte[tamanhoTotal];
        int posicaoAtual = 0;

        // Copiando os bytes de cada array no ArrayList para o array final
        for (byte[] array : conteudoTotalDividido) {
            System.arraycopy(array, 0, conteudoTotal, posicaoAtual, array.length);
            posicaoAtual += array.length;
        }

        return conteudoTotal;

    }




    public ArrayList<Diretorio> listarArquivos() {

        Diretorio d = new Diretorio();
        try {
            return d.deserializeDiretorios(this.disco);
        } catch (Exception e){
            return null;
        }


    }
}
