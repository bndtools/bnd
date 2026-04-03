package org.bndtools.refactor.ai.api;

public interface Embedder {

	float[] getEmbedding(String text);
}
