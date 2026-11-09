package com.example.itsm.intelligence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextSimilarityTest {

    @Test
    void identicalTextHasMaximumSimilarity() {
        assertThat(TextSimilarity.incidentSimilarity(
                "Payment application unavailable",
                "Customers cannot pay",
                "Payment application unavailable",
                "Customers cannot pay"
        )).isEqualTo(1.0);
    }

    @Test
    void unrelatedTextHasLowSimilarity() {
        assertThat(TextSimilarity.incidentSimilarity(
                "Laptop will not boot",
                "The screen is black",
                "Payment gateway returning 503",
                "Card transactions are failing"
        )).isLessThan(0.1);
    }

    @Test
    void overlapUsesTheSmallerTokenSet() {
        assertThat(TextSimilarity.overlap("payment gateway down", "payment gateway down now")).isEqualTo(1.0);
    }

    @Test
    void jaccardHandlesEmptyInput() {
        assertThat(TextSimilarity.jaccard("", "anything")).isEqualTo(0.0);
    }
}
