package org.apache.commons.math3.genetics;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Uniform random bits mutation applied over vector of double values.
 *
 * @author Todor Balabanov
 */
public class UniformBinaryMutation implements MutationPolicy {
    /**
     * Logger instance.
     */
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    /**
     * Pseudo random number generator.
     */
    private static final RandomGenerator PRNG =
            GeneticAlgorithm.getRandomGenerator();

    /**
     * {@inheritDoc}
     */
    @Override
    public Chromosome mutate(Chromosome original)
            throws MathIllegalArgumentException {
        if (original instanceof WeightsChromosome == false) {
            throw new MathIllegalArgumentException(
                    LocalizedFormats.INVALID_BINARY_CHROMOSOME);
        }

        /*
         * Deep copy of list values.
         */
        List<Double> representation = new ArrayList<Double>(
                ((WeightsChromosome) original).getRepresentation());

        /*
         * Mutate a single bit in each chromosome value.
         */
        for (int i = 0; i < representation.size(); i++) {
            double value = representation.get(i);
            if (PRNG.nextBoolean() == true) {
                value -= Double.MIN_VALUE;
            } else {
                value += Double.MIN_VALUE;
            }
            representation.set(i, value);
        }

        /*
         * Construct new chromosome after mutation.
         */
        return ((WeightsChromosome) original).
                newFixedLengthChromosome(representation);
    }
}
