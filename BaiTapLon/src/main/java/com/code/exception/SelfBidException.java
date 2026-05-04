package com.code.exception;

public class SelfBidException extends Exception {
    public SelfBidException() {
        super("Bạn không thể đặt giá cho sản phẩm của chính mình.");
    }
}

