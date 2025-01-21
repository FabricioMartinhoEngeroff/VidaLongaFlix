//package com.dvFabricio.VidaLongaFlix.infra.exception.common;
//
//public class AppException extends RuntimeException {
//
//    private int errorCode;  // Código do erro (opcional)
//    private String details; // Detalhes adicionais sobre o erro (opcional)
//
//    // Construtor com mensagem de erro
//    public AppException(String message) {
//        super(message);
//    }
//
//    // Construtor com mensagem de erro e código de erro
//    public AppException(String message, int errorCode) {
//        super(message);
//        this.errorCode = errorCode;
//    }
//
//    // Construtor com mensagem de erro, código de erro e detalhes
//    public AppException(String message, int errorCode, String details) {
//        super(message);
//        this.errorCode = errorCode;
//        this.details = details;
//    }
//
//    // Construtor com mensagem de erro e a causa da exceção
//    public AppException(String message, Throwable cause) {
//        super(message, cause);
//    }
//
//    // Construtor com mensagem, causa e código de erro
//    public AppException(String message, Throwable cause, int errorCode) {
//        super(message, cause);
//        this.errorCode = errorCode;
//    }
//
//    // Getters e setters para o código de erro e detalhes
//    public int getErrorCode() {
//        return errorCode;
//    }
//
//    public void setErrorCode(int errorCode) {
//        this.errorCode = errorCode;
//    }
//
//    public String getDetails() {
//        return details;
//    }
//
//    public void setDetails(String details) {
//        this.details = details;
//    }
//}