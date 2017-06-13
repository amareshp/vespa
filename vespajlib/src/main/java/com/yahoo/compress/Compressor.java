// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.compress;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import java.util.Arrays;
import java.util.Optional;

/**
 * Compressor which can compress and decompress in various formats.
 * This class is thread safe. Creating a reusable instance is faster than creating instances as needed.
 *
 * @author bratseth
 */
public class Compressor {

    private final CompressionType type;
    private final int level;
    private final double compressionThresholdFactor;
    private final int compressMinSizeBytes;

    private final LZ4Factory factory = LZ4Factory.fastestInstance();

    /** Creates a compressor with default settings. */
    public Compressor() {
        this(CompressionType.LZ4);
    }

    /** Creates a compressor with a default compression type. */
    public Compressor(CompressionType type) {
        this(type, 9, 0.95, 0);
    }

    /**
     * Creates a compressor.
     *
     * @param type the type of compression to use to compress data
     * @param level a number between 0 and 9 where a higher value means more compression
     * @param compressionThresholdFactor the compression factor we need to achieve to return the compressed data
     *                                   instead of raw data
     * @param compressMinSizeBytes the minimal input data size to perform compression
     */
    public Compressor(CompressionType type, int level, double compressionThresholdFactor, int compressMinSizeBytes) {
        this.type = type;
        this.level = level;
        this.compressionThresholdFactor = compressionThresholdFactor;
        this.compressMinSizeBytes = compressMinSizeBytes;
    }

    /** Returns the default compression type used by this */
    public CompressionType type() { return type; }

    /** Returns the compression level this will use - a number between 0 and 9 where higher means more compression  */
    public int level() { return level; }

    /** Returns the compression factor we need to achieve to return compressed rather than raw data */
    public double compressionThresholdFactor() { return compressionThresholdFactor; }

    /** Returns the minimal data size required to perform compression */
    public int compressMinSizeBytes() { return compressMinSizeBytes; }

    /**
     * Compresses some data
     *
     * @param requestedCompression the desired compression type, which will be used if the data is deemed suitable.
     *                             Not all the existing types are actually supported.
     * @param data the data to compress. This array is only read by this method.
     * @param uncompressedSize uncompressedSize the size in bytes of the data array. If this is not present, it is
     *                         assumed that the size is the same as the data array size, i.e that it is completely
     *                         filled with uncompressed data.
     * @return the compression result
     * @throws IllegalArgumentException if the compression type is not supported
     */
    public Compression compress(CompressionType requestedCompression, byte[] data, Optional<Integer> uncompressedSize) {
        switch (requestedCompression) {
            case NONE:
                data = uncompressedSize.isPresent() ? Arrays.copyOf(data, uncompressedSize.get()) : data;
                return new Compression(CompressionType.NONE, data.length, data);
            case LZ4:
                int dataSize = uncompressedSize.isPresent() ? uncompressedSize.get() : data.length;
                if (dataSize < compressMinSizeBytes) return new Compression(CompressionType.INCOMPRESSIBLE, dataSize, data);
                LZ4Compressor compressor = level < 7 ? factory.fastCompressor() : factory.highCompressor();
                byte[] compressedData = compressor.compress(data, 0, dataSize);
                if (compressedData.length + 8 >= dataSize * compressionThresholdFactor)
                    return new Compression(CompressionType.INCOMPRESSIBLE, dataSize, data);
                return new Compression(CompressionType.LZ4, dataSize, compressedData);
            default:
                throw new IllegalArgumentException(requestedCompression + " is not supported");
        }
    }
    /** Compresses some data using the compression type of this compressor */
    public Compression compress(CompressionType requestedCompression, byte[] data) { return compress(type, data, Optional.empty()); }
    /** Compresses some data using the compression type of this compressor */
    public Compression compress(byte[] data, int uncompressedSize) { return compress(type, data, Optional.of(uncompressedSize)); }
    /** Compresses some data using the compression type of this compressor */
    public Compression compress(byte[] data) { return compress(type, data, Optional.empty()); }

    /**
     * Decompresses some data
     *
     * @param compression the compression type used
     * @param compressedData the compressed data. This array is only read by this method.
     * @param compressedDataOffset the offset in the compressed data at which to start decompression
     * @param expectedUncompressedSize the uncompressed size in bytes of this data
     * @param expectedCompressedSize the expected compressed size of the data in bytes, optionally for validation with LZ4.
     * @return the uncompressed data, of the given size
     * @throws IllegalArgumentException if the compression type is not supported
     * @throws IllegalStateException if the expected compressed size is non-empty and specifies a different size than the actual size
     */
    public byte[] decompress(CompressionType compression, byte[] compressedData, int compressedDataOffset,
                             int expectedUncompressedSize, Optional<Integer> expectedCompressedSize) {
        switch (compression) {
            case NONE: case INCOMPRESSIBLE: // return a copy of the requested slize of the input buffer
                int endPosition = expectedCompressedSize.isPresent() ? compressedDataOffset + expectedCompressedSize.get() : compressedData.length;
                return Arrays.copyOfRange(compressedData, compressedDataOffset, endPosition);
            case LZ4:
                byte[] uncompressedLZ4Data = new byte[expectedUncompressedSize];
                int compressedSize = factory.fastDecompressor().decompress(compressedData, compressedDataOffset,
                                                                           uncompressedLZ4Data, 0, expectedUncompressedSize);
                if (expectedCompressedSize.isPresent() && compressedSize != expectedCompressedSize.get())
                    throw new IllegalStateException("Compressed size mismatch. Expected " + compressedSize + ". Got " + expectedCompressedSize.get());
                return uncompressedLZ4Data;
            default:
                throw new IllegalArgumentException(compression + " is not supported");
        }
    }
    /** Decompresses some data */
    public byte[] decompress(byte[] compressedData, CompressionType compressionType, int uncompressedSize) {
        return decompress(compressionType, compressedData, 0, uncompressedSize, Optional.empty());
    }
    /** Decompresses some data */
    public byte[] decompress(Compression compression) {
        return decompress(compression.type(), compression.data(), 0, compression.uncompressedSize(), Optional.empty());
    }

    public static class Compression {

        private final CompressionType compressionType;
        private final int uncompressedSize;
        private final byte[] data;

        public Compression(CompressionType compressionType, int uncompressedSize, byte[] data) {
            this.compressionType = compressionType;
            this.uncompressedSize = uncompressedSize;
            this.data = data;
        }

        /**
         * Returns the compression type used to compress this data.
         * This will be either the requested compression or INCOMPRESSIBLE.
         */
        public CompressionType type() { return compressionType; }
        
        /** Returns the uncompressed size of this data in bytes */
        public int uncompressedSize() { return uncompressedSize; }

        /** Returns the uncompressed data in a buffer which gets owned by the caller */
        public byte[] data() { return data; }

    }

}
