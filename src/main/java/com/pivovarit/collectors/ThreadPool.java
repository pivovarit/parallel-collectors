package com.pivovarit.collectors;

import java.util.concurrent.Executor;

record ThreadPool(Executor executor) implements Modification {
}
