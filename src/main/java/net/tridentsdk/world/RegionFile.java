/*
 * Copyright (c) 2014, TridentSDK Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of TridentSDK nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.tridentsdk.world;

import net.tridentsdk.api.nbt.*;

import java.io.*;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.zip.*;

import com.google.common.math.IntMath;

/**
 * Represents a Region File (in region/ directory) in memory
 */
public class RegionFile {
    //The path to the region file
    private final Path path;
    //The class in charge of sector allocation
    private final SectorStorage sectors;
    //The object to lock on to stop reading/writing simultaneously
    private final Object readWriteLock = new Object();

    public RegionFile(Path path)
            throws IOException, DataFormatException, NBTException {
        this.path = path;
        
        synchronized (readWriteLock) {
            RandomAccessFile access;
            //Checks whether or not the file exists
            if (!Files.isRegularFile(path)) {
                //Delete if it is mistakenly a directory
                Files.deleteIfExists(path);
                //Creates a new empty file
                Files.createFile(path);
                access  = new RandomAccessFile(path.toFile(), "rw");
                createNew(access);
            } else {
                access  = new RandomAccessFile(path.toFile(), "rw");
            }
            
            // Packing to default size of 8192 if it isn't already that size
            // (this should really never happen, but I'll take my changes)
            if (access.length() < 8192L) {
                access.seek(access.length());
    
                long diff = 8192L -access.length();
                for (long l = 0L; l < diff; l++) {
                    access.write(0);
                }
            }
            
            packFile(access);
            
            //Jump to beginning of file
            access.seek(0L);
            
            //Get the offsets of each chunk and cache
            int[] offsets = new int[1024];
            for (int i = 0; i < offsets.length ; i++) {
                offsets[i] = access.readInt();
            }
            sectors = new SectorStorage(offsets);
            
            access.close();
        }
        
    }
    
    /**
     * Packs the file with empty bytes in order to fit the specifications
     * The idea behind the packing is for speed (file systems work better with 4KiB chunks apparently)
     * @throws IOException
     */
    private void packFile(RandomAccessFile access) throws IOException {
        // Packing if length is not a multiple of 4096
        if ((access.length() % 4096L) != 0) {
            long paddingNeeded = access.length() % 4096L;
            
            //Jump to the end of the file
            access.seek(access.length());
            byte[] padding = new byte[(int) paddingNeeded];
            access.write(padding);
        }
        
        access.close();
    }
    
    private void createNew(RandomAccessFile access) {
        /*TODO: Generate a new Region File
         * - Start off with just blank files
         * - Move on to world generation
         */
       
    }
    
    /**
     * Pass in a chunk to load its data from file
     * 
     * @param chunk
     * @throws NBTException
     * @throws IOException
     * @throws DataFormatException
     */
    public void loadChunkData(TridentChunk chunk) throws NBTException, IOException, DataFormatException {
        short compression;
        byte[] compressedData;
        synchronized (readWriteLock) {
            RandomAccessFile access = new RandomAccessFile(path.toFile(), "rw");
    
            //Jump to timestamp location
            access.seek(sectors.getTimeStampLocation(chunk));
    
            // Read Timestamp
            int lastUpdate = access.readInt();
            
    
            // Check to see whether the chunk needs the data loaded
            // Not sure why it would ever not need
            if (chunk.getLastFileAccess() > lastUpdate) {
                chunk.setLastFileAccess((int) (System.currentTimeMillis()/1000));
                access.close();
                return;
            } else {
                chunk.setLastFileAccess((int) (System.currentTimeMillis()/1000));
            }
            
            //Jump to location of actual chunk data
            access.seek(sectors.getDataLocation(chunk));
    
            // Read the length, and the compression type
            int length = access.readInt();
            compression = (short) access.readByte();
            compressedData = new byte[length - 1];
    
            // Read the compressed data
            access.readFully(compressedData);
            
            //Close the stream as fast as possible: allows other chunks to read as soon as possible
            access.close();
        }
        // Decompress the data using rather the GZIP or Zlib
        byte[] chunkData;
        switch (compression) {
            case 0:
                
            case 1:
                GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(compressedData));
                chunkData = new byte[in.available()];
                in.read(chunkData);
                in.close();
                break;

            case 2:
                Inflater inflater = new Inflater();

                inflater.setInput(compressedData);
                chunkData = new byte[inflater.getRemaining()];

                inflater.inflate(chunkData);
                inflater.end();
                break;

            default:
                throw new IllegalStateException("Compression type provided is invalid!");
        }

        // Get the NBT tag
        CompoundTag nbtData = new NBTDecoder(new DataInputStream(new ByteArrayInputStream(chunkData))).decode();
        chunk.setData(nbtData);
        
        
    }
    
    /**
     * Pass in a chunk to save its data to the file
     * 
     * @param chunk
     * @throws NBTException
     * @throws IOException
     * @throws DataFormatException
     */
    public void saveChunkData(TridentChunk chunk) throws IOException, NBTException {
        /* Gets the ChunkData in a byte array form */
        ByteArrayOutputStream nbtStream = new ByteArrayOutputStream();
        new NBTEncoder(new DataOutputStream(new ByteArrayOutputStream())).encode(chunk.getData());
        byte[] uncompressed = nbtStream.toByteArray();
        
        /* Gonna only use Zlib compression by default */
        Deflater deflater = new Deflater();
        deflater.setInput(uncompressed);
        byte[] compressed = new byte[(int) deflater.getBytesRead()];
        deflater.deflate(compressed);
        
        /* Compare and sector lengths*/
        //The extra byte is for compression type (always saved as 1 for now)
        int actualLength = compressed.length + 1;
        //Sector length is rounded up to the nearest sector
        int sectorLength = IntMath.divide(actualLength, SectorStorage.SECTOR_LENGTH, RoundingMode.CEILING);
        //Checks if offsets need to change
        int oldSectorLength = sectors.getDataSectors(chunk);
        //If the length is smaller, we can free up a sector
        if (sectorLength < oldSectorLength) {
            sectors.setDataSectors(chunk, sectorLength);
            //Clears up all the now-free sectors
            sectors.freeSectors(sectors.getSectorOffset(chunk) + (sectorLength - 1), oldSectorLength - sectorLength);
        }
        //If the length is bigger, we need to find a new location!
        else if (sectorLength > oldSectorLength) {
            sectors.setDataSectors(chunk, sectorLength);
            
            //Clears up all the space previously used by this chunk (we need to find a new space!
            sectors.freeSectors(sectors.getSectorOffset(chunk), oldSectorLength);
            
            //Finds a new free location
            sectors.setSectorOffset(chunk, sectors.findFreeSectors(sectorLength));
        }
        //Update what sectors are being used
        sectors.addSectors(sectors.getSectorOffset(chunk), sectors.getDataSectors(chunk));
        
        synchronized (readWriteLock) {
            /* Write the actual chunk data */
            //Initialize access to the file
            RandomAccessFile access = new RandomAccessFile(path.toFile(), "rw");
            access.seek(sectors.getDataLocation(chunk));
            access.write(actualLength);
            //We only use compression type 1 (zlib)
            access.write((byte) 1);
            access.write(compressed);
            //Now we pad to the end of the sector... just in-case
            int paddingNeeded = actualLength % SectorStorage.SECTOR_LENGTH;
            if (paddingNeeded != 0) {
                byte[] padding = new byte[paddingNeeded];
                access.write(padding);
            }
            
            //Write the new offset data to the header
            access.seek(4 * sectors.getOffsetLoc(chunk));
            access.write(sectors.getRawOffset(chunk));
            
            //Pack the file as in the specifications
            packFile(access);
            
            //Finished writing the chunk
            access.close();
        }
    }
    
    
    
    /**
     * Class that manages the free space/sectors of the file
     */
    private static class SectorStorage {
        //The length of a Sector in bytes
        private final static int SECTOR_LENGTH = 4096;
        
        //The mapping of which sectors are free/not free (true is occupied)
        private final BitSet sectorMapping;
        //A cache of the locationOffsets
        private final int[] offsets;
        
        private SectorStorage(int[] offsets) {
            this.offsets = offsets;
            
            //Random starting capacity for now
            sectorMapping = new BitSet(1024);
            //The first two sectors are reserved for the header
            sectorMapping.set(0);
            sectorMapping.set(1);
            //Set what sectors are initially taken up
            for (int i = 0; i < offsets.length ; i++) {
                int loc = offsets[i] >> 8;
                int length = offsets[i] & 0xFF;
                for (int j = loc; j < loc + length; j++) {
                    sectorMapping.set(j);
                }
            }
        }
        
        /**
         * Finds a section of the file with enough free-space to accomodate 'length' sectors of data
         * 
         * @param length the amount of sectors of data
         * @return offset the location of the free space (in sectors)
         */
        int findFreeSectors(int length) {
            boolean found = false;
            //Start searching after the header
            int counter = 2;
            int consecutive = 0;
            while (!found) {
                if (!sectorMapping.get(counter)) {
                    consecutive++;
                    if (consecutive >= length) {
                        found = true;
                        break;
                    }
                } else {
                    consecutive = 0;
                }
            }
            
            return counter;
            
        }
        
        /**
         * Free up a certain length of sectors, from a starting point
         * 
         * @param start the sector to start from (inclusive)
         * @param length the amount of sectors to free up
         */
        void freeSectors(int start, int length) {
            for (int i = start; i < start + length; i++) {
                sectorMapping.set(i, false);
            }
        }
        
        /**
         * Occupies up a certain length of sectors, from a starting point
         * 
         * @param start the sector to start from (inclusive)
         * @param length the amount of sectors to occupy
         */
        void addSectors(int start, int length) {
            for (int i = start; i < start + length; i++) {
                sectorMapping.set(i, true);
            }
        }
        
        /**
         * Gets the location of a chunk's timestamp 
         * 
         * @param c chunk
         * @return location in bytes
         */
        int getTimeStampLocation(TridentChunk c) {
            return 4 * getOffsetLoc(c) + SECTOR_LENGTH;
        }
        
        /**
         * Gets the location of a chunk's data 
         * 
         * @param c chunk
         * @return location in bytes
         */
        int getDataLocation(TridentChunk c) {
            return getSectorOffset(c) * SECTOR_LENGTH;
        }
        
        /**
         * Gets the amount of sectors a chunk occupies in the file
         * 
         * @param c chunk
         * @return amount the amount of sectors
         */
        int getDataSectors(TridentChunk c) {
            return offsets[getOffsetLoc(c)] & 0xFF;
        }
        
        /**
         * Sets the amount of sectors a chunk occupies
         * 
         * @param c chunk
         * @param toSet the amount to set
         */
        void setDataSectors(TridentChunk c, int toSet) {
            int old = offsets[getOffsetLoc(c)];
            offsets[getOffsetLoc(c)] = (old & 0xFFFFFF00) | toSet;
        }
        
        /**
         * Gets the offset of the chunk data in Sectors
         * 
         * @param c chunk
         * @return amount the amount of sectors
         */
        int getSectorOffset(TridentChunk c) {
           return offsets[getOffsetLoc(c)] >> 8;
        }
        
        /**
         * Sets the offset of the chunk data in Sectors
         * 
         * @param c chunk
         * @param toSet the amount to set
         */
        void setSectorOffset(TridentChunk c, int toSet) {
            int old = offsets[getOffsetLoc(c)];
            offsets[getOffsetLoc(c)] = (old & 0x000000FF) | (toSet << 8);
         }
        
        /**
         * Gets the raw header/offset of the chunk
         * 
         * @param c chunk
         * @return offset
         */
        private int getRawOffset(TridentChunk c) {
            return offsets[getOffsetLoc(c)];
        }
        
        /**
         * Gets the location of the raw offset of the chunk
         * 
         * @param c chunk
         * @return offsetLoc in bytes
         */
        private int getOffsetLoc(TridentChunk c) {
            return (IntMath.mod(c.getX(), 32) + IntMath.mod(c.getX(), 32) * 32);
        }
    }
}
