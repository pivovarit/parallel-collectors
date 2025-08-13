package com.pivovarit.collectors;

import java.util.concurrent.FutureTask;

class Main {

    public static void main(String[] args) {
        FutureTask<String> t = new FutureTask<>(() -> {
            System.out.println("running");
            return "";
        });
        t.cancel(true);
        t.run();
    }
}
