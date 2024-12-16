package edu.so;

public interface FileSystem {

    /**
     * Cria um novo arquivo.
     * @param fileName nome do arquivo para criar
     * @param data dados a serem salvos
     */
    void create(String fileName, byte[] data);
    /**
     * Adiciona dados ao final do arquivo.
     * @param fileName nome do arquivo
     * @param data dados a serem adicionados
     */
    void append(String fileName, byte[] data);
    /**
     * Lê arquivo.
     * @param fileName nome do arquivo
     */
    void read(String fileName);
    /**
     * Remove o arquivo.
     * @param fileName
     */
    void remove(String fileName);
    /**
     * Calcula o espaço disponível no sistema de arquivos.
     * @return bytes disponíveis
     */
    int calculaEspacoLivre();

}
