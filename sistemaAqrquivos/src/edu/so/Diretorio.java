package edu.so;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class Diretorio implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nomeArquivo;
    private String extensao;
    private int tamanho;
    private int blocoInicial;

    private List<Diretorio> diretorio = new ArrayList<>();

    public Diretorio() {

    }

    public Diretorio(String nomeArquivo,
                     String extensao,
                     int tamanho,
                     int blocoInicial) {
        this.nomeArquivo = nomeArquivo;
        if (this.nomeArquivo.length() > 8) {
            this.nomeArquivo = nomeArquivo.substring(0, 8);
        } else if (this.nomeArquivo.length() < 8) {
            do {
                this.nomeArquivo += " ";
            } while (this.nomeArquivo.length() < 8);
        }
        this.extensao = extensao;
        if (this.extensao.length() > 3) {
            this.extensao = extensao.substring(0, 3);
        } else if (this.extensao.length() < 3) {
            do {
                this.extensao += " ";
            } while (this.extensao.length() < 3);
        }
        this.tamanho = tamanho;
        this.blocoInicial = blocoInicial;
        if (blocoInicial < 2 || blocoInicial >= Disco.NUM_BLOCOS) {
            throw new IllegalArgumentException("numero de bloco invalido");
        }
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public String getExtensao() {
        return extensao;
    }

    public void setExtensao(String extensao) {
        this.extensao = extensao;
    }

    public int getTamanho() {
        return tamanho;
    }

    public void setTamanho(int tamanho) {
        this.tamanho = tamanho;
    }

    public int getBlocoInicial() {
        return blocoInicial;
    }

    public void setBlocoInicial(int blocoInicial) {
        this.blocoInicial = blocoInicial;
    }

    public List<Diretorio> getDiretorio() {
        return diretorio;
    }

    public void setDiretorio(List<Diretorio> diretorio) {
        this.diretorio = diretorio;
    }

    public static void escreveDiretorio(ArrayList<Diretorio> arquivos, Disco disco) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(64*1024);
        for (Diretorio arq : arquivos) {
            bb.put(arq.toByteArray());
        }
        byte[] blocoDiretorio = bb.array();
        disco.escreveBloco(0, blocoDiretorio);

    }

//--------------------------------------------------------------------------------

    public static void serializeDiretorios(ArrayList<Diretorio> diretorios, Disco disco) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(diretorios);
        oos.flush();
        disco.escreveBloco(0, bos.toByteArray());
    }

    public static ArrayList<Diretorio> deserializeDiretorios(Disco disco) throws IOException, ClassNotFoundException {
        System.out.println("k");
        byte[] data = disco.leBloco(0);
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (ArrayList<Diretorio>) ois.readObject();
    }


    public static void limparDiretorio(Disco disco) throws IOException {

        try{
            ArrayList<Diretorio> diretorios = deserializeDiretorios(disco);
            diretorios.clear();
            serializeDiretorios(diretorios, disco);
        } catch (Exception e){

        }


    }




    //_________________________________________________________________________

    public byte[] toByteArray() {
        ByteBuffer bb = ByteBuffer.allocate(19);
        bb.put(nomeArquivo.getBytes(StandardCharsets.ISO_8859_1));
        bb.put(extensao.getBytes(StandardCharsets.ISO_8859_1));
        bb.putInt(tamanho);
        bb.putInt(blocoInicial);
        return bb.array();
    }

    public ArrayList<Diretorio>  leDiretorio(Disco disco) throws IOException {

        ArrayList<Diretorio> d = new ArrayList<>();
        byte[] blocoDiretorio = disco.leBloco(0);
        ByteBuffer bb = ByteBuffer.wrap(blocoDiretorio);
        while (bb.hasRemaining()) {
            byte[] entradaBytes = new byte[19];
            bb.get(entradaBytes);
            Diretorio entrada = Diretorio.fromBytes(entradaBytes);
            if (!entrada.nomeArquivo.trim().isEmpty()) {
                d.add(entrada);
                System.out.println("haha2");
            }

        }
        System.out.println("haha3");
        return d;

    }

    public static Diretorio fromBytes(byte[] bytes) {
        String nome = new String(bytes,
                0, 8, StandardCharsets.ISO_8859_1);
        String extensao = new String(bytes,
                8, 3, StandardCharsets.ISO_8859_1);
        int tamanho = intFromBytes(bytes, 11);
        int blocoInicial = intFromBytes(bytes, 15);
        System.out.println(nome);
        System.out.println(extensao);
        System.out.println(tamanho);
        System.out.println(blocoInicial);
        System.out.println("haha");
        return new Diretorio(nome, extensao, tamanho, blocoInicial);
    }

    public static Diretorio fromStream(InputStream inputStream) throws IOException {
        byte[] bytes = new byte[19];
        inputStream.read(bytes);
        String nome = new String(bytes,
                0, 8, StandardCharsets.ISO_8859_1);
        String extensao = new String(bytes,
                8, 3, StandardCharsets.ISO_8859_1);
        int tamanho = intFromBytes(bytes, 11);
        int blocoInicial = intFromBytes(bytes, 15);
        System.out.println(nome);
        System.out.println(extensao);
        System.out.println(tamanho);
        System.out.println(blocoInicial);
        return new Diretorio(nome, extensao, tamanho, blocoInicial);
    }

    private static int intFromBytes(byte[] data, int index) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        return bb.getInt(index);
    }

    @Override
    public String toString() {
        return "Diretorio{" +
                "nomeArquivo='" + nomeArquivo + '\'' +
                ", extensao='" + extensao + '\'' +
                ", tamanho=" + tamanho +
                ", blocoInicial=" + blocoInicial +
                "}";
    }
}
