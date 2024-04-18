/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

/**
 * This class defines the Service Provider Interface (SPI) for the {@link KEM}
 * class. A security provider implements this interface to provide an
 * implementation of a Key Encapsulation Mechanism (KEM) algorithm.
 * <p>
 * A KEM algorithm may support a family of configurations. Each configuration
 * may accept different types of keys, cryptographic primitives, and sizes of
 * shared secrets and key encapsulation messages. A configuration is defined
 * by the KEM algorithm name, the key it uses, and an optional
 * {@code AlgorithmParameterSpec} argument that is specified when creating
 * an encapsulator or decapsulator. The result of calling
 * {@link #engineNewEncapsulator} or {@link #engineNewDecapsulator} must return
 * an encapsulator or decapsulator that maps to a single configuration,
 * where its {@code engineSecretSize()} and {@code engineEncapsulationSize()}
 * methods return constant values.
 * <p>
 * A {@code KEMSpi} implementation must be immutable. It must be safe to
 * call multiple {@code engineNewEncapsulator} and {@code engineNewDecapsulator}
 * methods at the same time.
 * <p>
 * {@code EncapsulatorSpi} and {@code DecapsulatorSpi} implementations must also
 * be immutable. It must be safe to invoke multiple {@code encapsulate} and
 * {@code decapsulate} methods at the same time. Each invocation of
 * {@code encapsulate} should generate a new shared secret and key
 * encapsulation message.
 * <p>
 * For example,
 * <pre>{@code
 * public static class MyKEMImpl implements KEMSpi {
 *
 *     @Override
 *     public KEMSpi.EncapsulatorSpi engineNewEncapsulator(PublicKey publicKey,
 *             AlgorithmParameterSpec spec, SecureRandom secureRandom)
 *             throws InvalidAlgorithmParameterException, InvalidKeyException {
 *         if (!checkPublicKey(publicKey)) {
 *             throw new InvalidKeyException("unsupported key");
 *         }
 *         if (!checkParameters(spec)) {
 *             throw new InvalidAlgorithmParameterException("unsupported params");
 *         }
 *         return new MyEncapsulator(publicKey, spec, secureRandom);
 *     }
 *
 *     class MyEncapsulator implements KEMSpi.EncapsulatorSpi {
 *         MyEncapsulator(PublicKey publicKey, AlgorithmParameterSpec spec,
 *                 SecureRandom secureRandom){
 *             this.spec = spec != null ? spec : getDefaultParameters();
 *             this.secureRandom = secureRandom != null
 *                     ? secureRandom
 *                     : getDefaultSecureRandom();
 *             this.publicKey = publicKey;
 *         }
 *
 *         @Override
 *         public KEM.Encapsulated encapsulate(int from, int to, String algorithm) {
 *             byte[] encapsulation;
 *             byte[] secret;
 *             // calculating...
 *             return new KEM.Encapsulated(
 *                     new SecretKeySpec(secret, from, to - from, algorithm),
 *                     encapsulation, null);
 *         }
 *
 *         // ...
 *     }
 *
 *     // ...
 * }
 * }</pre>
 *
 * @see KEM
 * @since 21
 */
public interface KEMSpi {

    /**
     * The KEM encapsulator implementation, generated by
     * {@link #engineNewEncapsulator} on the KEM sender side.
     *
     * @see KEM.Encapsulator
     *
     * @since 21
     */
    interface EncapsulatorSpi {
        /**
         * The key encapsulation function.
         * <p>
         * Each invocation of this method must generate a new secret key and key
         * encapsulation message that is returned in an {@link KEM.Encapsulated} object.
         * <p>
         * An implementation must support the case where {@code from} is 0,
         * {@code to} is the same as the return value of {@code secretSize()},
         * and {@code algorithm} is "Generic".
         *
         * @param from the initial index of the shared secret byte array
         *          to be returned, inclusive
         * @param to the final index of the shared secret byte array
         *          to be returned, exclusive
         * @param algorithm the algorithm name for the secret key that is returned
         * @return an {@link KEM.Encapsulated} object containing a portion of
         *          the shared secret as a key with the specified algorithm,
         *          key encapsulation message, and optional parameters.
         * @throws IndexOutOfBoundsException if {@code from < 0},
         *     {@code from > to}, or {@code to > secretSize()}
         * @throws NullPointerException if {@code algorithm} is {@code null}
         * @throws UnsupportedOperationException if the combination of
         *          {@code from}, {@code to}, and {@code algorithm}
         *          is not supported by the encapsulator
         * @see KEM.Encapsulated
         * @see KEM.Encapsulator#encapsulate(int, int, String)
         */
        KEM.Encapsulated engineEncapsulate(int from, int to, String algorithm);

        /**
         * Returns the size of the shared secret.
         *
         * @return the size of the shared secret as a finite non-negative integer
         * @see KEM.Encapsulator#secretSize()
         */
        int engineSecretSize();

        /**
         * Returns the size of the key encapsulation message.
         *
         * @return the size of the key encapsulation message as a finite non-negative integer
         * @see KEM.Encapsulator#encapsulationSize()
         */
        int engineEncapsulationSize();
    }

    /**
     * The KEM decapsulator implementation, generated by
     * {@link #engineNewDecapsulator} on the KEM receiver side.
     *
     * @see KEM.Decapsulator
     *
     * @since 21
     */
    interface DecapsulatorSpi {
        /**
         * The key decapsulation function.
         * <p>
         * An invocation of this method recovers the secret key from the key
         * encapsulation message.
         * <p>
         * An implementation must support the case where {@code from} is 0,
         * {@code to} is the same as the return value of {@code secretSize()},
         * and {@code algorithm} is "Generic".
         *
         * @param encapsulation the key encapsulation message from the sender.
         *          The size must be equal to the value returned by
         *          {@link #engineEncapsulationSize()} ()}, or a
         *          {@code DecapsulateException} must be thrown.
         * @param from the initial index of the shared secret byte array
         *          to be returned, inclusive
         * @param to the final index of the shared secret byte array
         *          to be returned, exclusive
         * @param algorithm the algorithm name for the secret key that is returned
         * @return a portion of the shared secret as a {@code SecretKey} with
         *          the specified algorithm
         * @throws DecapsulateException if an error occurs during the
         *          decapsulation process
         * @throws IndexOutOfBoundsException if {@code from < 0},
         *          {@code from > to}, or {@code to > secretSize()}
         * @throws NullPointerException if {@code encapsulation} or
         *          {@code algorithm} is {@code null}
         * @throws UnsupportedOperationException if the combination of
         *          {@code from}, {@code to}, and {@code algorithm}
         *          is not supported by the decapsulator
         * @see KEM.Decapsulator#decapsulate(byte[], int, int, String)
         */
        SecretKey engineDecapsulate(byte[] encapsulation, int from, int to, String algorithm)
                throws DecapsulateException;

        /**
         * Returns the size of the shared secret.
         *
         * @return the size of the shared secret as a finite non-negative integer
         * @see KEM.Decapsulator#secretSize()
         */
        int engineSecretSize();

        /**
         * Returns the size of the key encapsulation message.
         *
         * @return the size of the key encapsulation message as a finite non-negative integer
         * @see KEM.Decapsulator#encapsulationSize()
         */
        int engineEncapsulationSize();
    }

    /**
     * Creates a KEM encapsulator on the KEM sender side.
     *
     * @param publicKey the receiver's public key, must not be {@code null}
     * @param spec the optional parameter, can be {@code null}
     * @param secureRandom the source of randomness for encapsulation.
     *                     If {@code null}, the implementation must provide
     *                     a default one.
     * @return the encapsulator for this key
     * @throws InvalidAlgorithmParameterException if {@code spec} is invalid
     *          or one is required but {@code spec} is {@code null}
     * @throws InvalidKeyException if {@code publicKey} is {@code null} or invalid
     * @see KEM#newEncapsulator(PublicKey, AlgorithmParameterSpec, SecureRandom)
     */
    EncapsulatorSpi engineNewEncapsulator(PublicKey publicKey,
            AlgorithmParameterSpec spec, SecureRandom secureRandom)
            throws InvalidAlgorithmParameterException, InvalidKeyException;

    /**
     * Creates a KEM decapsulator on the KEM receiver side.
     *
     * @param privateKey the receiver's private key, must not be {@code null}
     * @param spec the optional parameter, can be {@code null}
     * @return the decapsulator for this key
     * @throws InvalidAlgorithmParameterException if {@code spec} is invalid
     *          or one is required but {@code spec} is {@code null}
     * @throws InvalidKeyException if {@code privateKey} is {@code null} or invalid
     * @see KEM#newDecapsulator(PrivateKey, AlgorithmParameterSpec)
     */
    DecapsulatorSpi engineNewDecapsulator(PrivateKey privateKey, AlgorithmParameterSpec spec)
            throws InvalidAlgorithmParameterException, InvalidKeyException;
}
