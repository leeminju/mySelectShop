package com.sparta.myselectshop.exception;

public class ProductNotFoundException extends RuntimeException{
    public ProductNotFoundException(String message) {
        super(message);//부모로 메세지 전달
    }
}
