package com.jagrosh.jmusicbot.pun;

public class PunException extends Exception {

    public PunException(String errorMessage) {
        super(errorMessage);
    }

    public PunException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}