package com.limegroup.gnutella;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.limewire.collection.BitNumbers;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.NameValue;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IPPortCombo;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteOrder;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.HUGEExtension;
import com.limegroup.gnutella.messages.IntervalEncoder;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLNames;

@Singleton
public class ResponseFactoryImpl implements ResponseFactory {

    /**
     * The maximum number of alternate locations to include in responses in the
     * GGEP block
     */
    private static final int MAX_LOCATIONS = 10;

    /** The magic byte to use as extension separators. */
    private static final byte EXT_SEPARATOR = 0x1c;

    /** Constant for the KBPS string to avoid constructing it too many times. */
    private static final String KBPS = "kbps";

    /** Constant for kHz to string to avoid excessive string construction. */
    private static final String KHZ = "kHz";

    private final AltLocManager altLocManager;
    private final Provider<CreationTimeCache> creationTimeCache;
    private final IPFilter ipFilter;
    private final NetworkInstanceUtils networkInstanceUtils;

    private final LimeXMLDocumentFactory limeXMLDocumentFactory;

    @Inject
    public ResponseFactoryImpl(AltLocManager altLocManager,
            Provider<CreationTimeCache> creationTimeCache, IPFilter ipFilter,
            LimeXMLDocumentFactory limeXMLDocumentFactory, NetworkInstanceUtils networkInstanceUtils) {
        this.altLocManager = altLocManager;
        this.creationTimeCache = creationTimeCache;
        this.ipFilter = ipFilter;
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.networkInstanceUtils = networkInstanceUtils;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ResponseFactory#createResponse(long, long, java.lang.String)
     */
    public Response createResponse(long index, long size, String name) {
        return createResponse(index, size, name, -1, null, null, null, null);
    }
  
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ResponseFactory#createResponse(long, long, java.lang.String, com.limegroup.gnutella.xml.LimeXMLDocument)
     */
    public Response createResponse(long index, long size, String name,
            LimeXMLDocument doc) {
        return createResponse(index, size, name, -1, null, doc, null, null);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ResponseFactory#createResponse(com.limegroup.gnutella.FileDesc)
     */
    public Response createResponse(FileDesc fd) {
        IntervalSet ranges = null;
        boolean verified = false;
        if (fd instanceof IncompleteFileDesc) {
            IncompleteFileDesc ifd = (IncompleteFileDesc)fd;
            ranges = new IntervalSet();
            verified = ifd.loadResponseRanges(ranges);
        }
        
        GGEPContainer container = new GGEPContainer(getAsIpPorts(altLocManager
                .getDirect(fd.getSHA1Urn())), creationTimeCache.get()
                .getCreationTimeAsLong(fd.getSHA1Urn()), fd.getFileSize(), ranges, 
                verified, fd.getTTROOTUrn());

        return createResponse(fd.getIndex(), fd.getFileSize(),
                fd.getFileName(), -1, fd.getUrns(), null, container, null);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ResponseFactory#createFromStream(java.io.InputStream)
     */
    public Response createFromStream(InputStream is) throws IOException {
        // extract file index & size
        long index = ByteOrder.uint2long(ByteOrder.leb2int(is));
        long size = ByteOrder.uint2long(ByteOrder.leb2int(is));

        int incomingNameByteArraySize;
        
        if ((index & 0xFFFFFFFF00000000L) != 0)
            throw new IOException("invalid index: " + index);
        if (size < 0)
            throw new IOException("invalid size: " + size);

        // The file name is terminated by a null terminator.
        // A second null indicates the end of this response.
        // Gnotella & others insert meta-information between
        // these null characters. So we have to handle this.
        // See:
        //  http://gnutelladev.wego.com/go/wego.discussion.message?groupId=139406&view=message&curMsgId=319258&discId=140845&index=-1&action=view

        
        // Extract the filename
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != 0) {
            if (c == -1)
                throw new IOException("EOF before null termination");
            baos.write(c);
        }
        
        incomingNameByteArraySize = baos.size();
        
		String name = new String(baos.toByteArray(), "UTF-8");    
        checkFilename(name); // throws IOException
		
        // Extract extra info, if any
        baos.reset();
        while ((c = is.read()) != 0) {
            if (c == -1)
                throw new IOException("EOF before null termination");
            baos.write(c);
        }
        byte[] rawMeta = baos.toByteArray();
        if (rawMeta.length == 0) {
            if (is.available() < 16) {
                throw new IOException("not enough room for the GUID");
            }
            //changed to pass an additional parameter (incomingNameByteArraySize) for the new response
            return createResponse(index, size, name, incomingNameByteArraySize, null, null, null, null);
        } else {
            // now handle between-the-nulls
            // \u001c is the HUGE v0.93 GEM delimiter
            HUGEExtension huge = new HUGEExtension(rawMeta);

            Set<URN> urns = huge.getURNS();

            LimeXMLDocument doc = null;
            for (String next : huge.getMiscBlocks()) {
                doc = createXmlDocument(name, next);
                if (doc != null)
                    break;
            }

            GGEPContainer ggep = getGGEP(huge.getGGEP(), size);
            if (ggep.size64 > MAX_FILE_SIZE)
                throw new IOException(" file too large " + ggep.size64);
            if (ggep.size64 > Integer.MAX_VALUE)
                size = ggep.size64;

            //changed to pass an additional parameter (baosByteArraySize) for the new response
            return createResponse(index, size, name, incomingNameByteArraySize, urns, doc, ggep, rawMeta);
        }
    }

    /**
     * Overloaded constructor that allows the creation of Responses with
     * meta-data and a <tt>Set</tt> of <tt>URN</tt> instances. This is the
     * primary constructor that establishes all of the class's invariants, does
     * any necessary parameter validation, etc.
     * 
     * If extensions is non-null, it is used as the extBytes instead of creating
     * them from the urns and locations.
     * 
     * @param index the index of the file referenced in the response
     * @param size the size of the file (in bytes)
     * @param name the name of the file
     * @param incomingNameByteArraySize TODO
     * @param urns the <tt>Set</tt> of <tt>URN</tt> instances associated
     *        with the file
     * @param doc the <tt>LimeXMLDocument</tt> instance associated with the
     *        file
     * @param extensions The raw unparsed extension bytes.
     * @param endpoints a collection of other locations on this network that
     *        will have this file
     */
    // NOTE: not in the interface, but public so tests not in this package can use.
    public Response createResponse(long index, long size, String name, int incomingNameByteArraySize,
            Set<? extends URN> urns, LimeXMLDocument doc,
            GGEPContainer ggepData, byte[] extensions) {
        
        // make sure ggepData is correct.
        if (ggepData == null) {
            if (size <= Integer.MAX_VALUE)
                ggepData = GGEPContainer.EMPTY;
            else // large filesizes require GGEP now
                ggepData = new GGEPContainer(null, -1L, size, null, false, null);
        }

        // build up extensions if it wasn't already!
        if (extensions == null)
            extensions = createExtBytes(urns, ggepData, size);

        return new Response(index, size, name, incomingNameByteArraySize, urns, doc,
                ggepData.locations, ggepData.createTime, extensions, ggepData.ranges, ggepData.verified);
    }
    
    private void checkFilename(String name) throws IOException {
        if (name.length() == 0) {
            throw new IOException("empty name in response");
        }
        // sanity checks for filename 
        if (name.indexOf('/') != -1 || name.indexOf('\n') != -1 || name.indexOf('\r') != -1) {
            throw new IOException("Illegal filename " + name + "contains one of [/\\n\\r]");
        }
    }

    /**
     * Constructs an xml string from the given extension sting.
     * 
     * @param name the name of the file to construct the string for
     * @param ext an individual between-the-nulls string (note that there can be
     *        multiple between-the-nulls extension strings with HUGE)
     * @return the xml formatted string, or the empty string if the xml could
     *         not be parsed
     */
    private LimeXMLDocument createXmlDocument(String name, String ext) {
        StringTokenizer tok = new StringTokenizer(ext);
        // if there aren't the expected number of tokens, simply
        // return the empty string
        if (tok.countTokens() < 2)
            return null;

        String first = tok.nextToken();
        String second = tok.nextToken();
        assert first != null;
        assert second != null;
        first = first.toLowerCase();
        second = second.toLowerCase();
        String length = "";
        String bitrate = "";
        boolean bearShare1 = false;
        boolean bearShare2 = false;
        boolean gnotella = false;
        if (second.startsWith(KBPS))
            bearShare1 = true;
        else if (first.endsWith(KBPS))
            bearShare2 = true;
        if (bearShare1) {
            bitrate = first;
        } else if (bearShare2) {
            int j = first.indexOf(KBPS);
            bitrate = first.substring(0, j);
        }
        if (bearShare1 || bearShare2) {
            while (tok.hasMoreTokens())
                length = tok.nextToken();
            // OK we have the bitrate and the length
        } else if (ext.endsWith(KHZ)) {// Gnotella
            gnotella = true;
            length = first;
            // extract the bitrate from second
            int i = second.indexOf(KBPS);
            if (i > -1)// see if we can find the bitrate
                bitrate = second.substring(0, i);
            else
                // not gnotella, after all...some other format we do not know
                gnotella = false;
        }

        // make sure these are valid numbers.
        try {
            Integer.parseInt(bitrate);
            Integer.parseInt(length);
        } catch (NumberFormatException nfe) {
            return null;
        }

        if (bearShare1 || bearShare2 || gnotella) {// some metadata we understand
            List<NameValue<String>> values = new ArrayList<NameValue<String>>(3);
            values.add(new NameValue<String>(LimeXMLNames.AUDIO_TITLE, name));
            values.add(new NameValue<String>(LimeXMLNames.AUDIO_BITRATE, bitrate));
            values.add(new NameValue<String>(LimeXMLNames.AUDIO_SECONDS, length));
            return limeXMLDocumentFactory.createLimeXMLDocument(values,
                    LimeXMLNames.AUDIO_SCHEMA);
        }

        return null;
    }

    /**
     * Helper method that creates an array of bytes for the specified
     * <tt>Set</tt> of <tt>URN</tt> instances. The bytes are written as
     * specified in HUGE v 0.94.
     * 
     * @param urns the <tt>Set</tt> of <tt>URN</tt> instances to use in
     *        constructing the byte array
     */
    private byte[] createExtBytes(Set<? extends URN> urns, GGEPContainer ggep, long size) {
        try {
            if (isEmpty(urns) && ggep.isEmpty())
                return DataUtils.EMPTY_BYTE_ARRAY;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (!isEmpty(urns)) {
                // Add the extension for URNs, if any.
                for (Iterator<? extends URN> iter = urns.iterator(); iter
                        .hasNext();) {
                    URN urn = iter.next();
                    assert urn != null : "Null URN";
                    
                    if (!urn.isSHA1() && MessageSettings.TTROOT_IN_GGEP.getValue()) 
                        continue;
                    
                    baos.write(urn.toString().getBytes());
                    // If there's another URN, add the separator.
                    if (iter.hasNext()) {
                        baos.write(EXT_SEPARATOR);
                    }
                }
                
                assert !(ggep.isEmpty() && urns.size() > 1);

                // If there's ggep data, write the separator.
                if (!ggep.isEmpty())
                    baos.write(EXT_SEPARATOR);
            }

            // It is imperitive that GGEP is added LAST.
            // That is because GGEP can contain 0x1c (EXT_SEPARATOR)
            // within it, which would cause parsing problems
            // otherwise.
            if (!ggep.isEmpty())
                addGGEP(baos, ggep, size);

            return baos.toByteArray();
        } catch (IOException impossible) {
            ErrorService.error(impossible);
            return DataUtils.EMPTY_BYTE_ARRAY;
        }
    }

    /**
     * Utility method to know if a set is empty or null.
     */
    private boolean isEmpty(Set<?> set) {
        return set == null || set.isEmpty();
    }

    /**
     * Utility method for converting the non-firewalled elements of an
     * AlternateLocationCollection to a smaller set of endpoints.
     */
    private Set<? extends IpPort> getAsIpPorts(
            AlternateLocationCollection<DirectAltLoc> col) {
        if (col == null || !col.hasAlternateLocations())
            return Collections.emptySet();

        long now = System.currentTimeMillis();
        synchronized (col) {
            Set<IpPort> endpoints = null;
            int i = 0;
            for (Iterator<DirectAltLoc> iter = col.iterator(); iter.hasNext()
                    && i < MAX_LOCATIONS;) {
                DirectAltLoc al = iter.next();
                if (al.canBeSent(AlternateLocation.MESH_RESPONSE)) {
                    IpPort host = al.getHost();
                    if (!networkInstanceUtils.isMe(host)) {
                        if (endpoints == null)
                            endpoints = new IpPortSet();

                        endpoints.add(host);
                        i++;
                        al.send(now, AlternateLocation.MESH_RESPONSE);
                    }
                } else if (!al.canBeSentAny())
                    iter.remove();
            }
            if (endpoints == null)
                return Collections.emptySet();
            else
                return endpoints;
        }
    }

    /**
     * Adds a GGEP block with the specified alternate locations to the output
     * stream.
     */
    private void addGGEP(OutputStream out, GGEPContainer ggep, long size)
            throws IOException {
        if (ggep == null
                || (ggep.locations.size() == 0 && 
                        ggep.createTime <= 0 && 
                        ggep.size64 <= Integer.MAX_VALUE &&
                        ggep.ranges == null &&
                        ggep.ttroot == null))
            throw new IllegalArgumentException(
                    "null or empty locations and small size");

        GGEP info = new GGEP(true);
        if (ggep.locations.size() > 0) {
            byte[] output = NetworkUtils.packIpPorts(ggep.locations);
            info.put(GGEP.GGEP_HEADER_ALTS, output);
            BitNumbers bn = HTTPHeaderUtils.getTLSIndices(ggep.locations);
            if (!bn.isEmpty())
                info.put(GGEP.GGEP_HEADER_ALTS_TLS, bn.toByteArray());
        }

        if (ggep.createTime > 0)
            info.put(GGEP.GGEP_HEADER_CREATE_TIME, ggep.createTime / 1000);

        if (ggep.size64 > Integer.MAX_VALUE && ggep.size64 <= MAX_FILE_SIZE)
            info.put(GGEP.GGEP_HEADER_LARGE_FILE, ggep.size64);
        
        if (ggep.ranges != null) {
            IntervalEncoder.encode(size, info, ggep.ranges);
            if (!ggep.verified)
                info.put(GGEP.GGEP_HEADER_PARTIAL_RESULT_UNVERIFIED);
        }
        
        if (ggep.ttroot != null && MessageSettings.TTROOT_IN_GGEP.getValue())
            info.put(GGEP.GGEP_HEADER_TTROOT,ggep.ttroot.getBytes());

        info.write(out);
    }

    /**
     * Returns a <tt>Set</tt> of other endpoints described in one of the GGEP
     * arrays.
     * 
     * Default access for testing.
     */
    GGEPContainer getGGEP(GGEP ggep, long size) {
        if (ggep == null)
            return GGEPContainer.EMPTY;

        Set<? extends IpPort> locations = null;
        long createTime = -1;
        long size64 = size;
        URN ttroot = null;

        // if the block has a ALTS value, get it, parse it,
        // and move to the next.
        if (ggep.hasKey(GGEP.GGEP_HEADER_ALTS)) {
            byte[] tlsData = null;
            if (ggep.hasKey(GGEP.GGEP_HEADER_ALTS_TLS)) {
                try {
                    tlsData = ggep.getBytes(GGEP.GGEP_HEADER_ALTS_TLS);
                } catch (BadGGEPPropertyException ignored) {
                }
            }
            BitNumbers bn = tlsData == null ? null : new BitNumbers(tlsData);
            try {
                locations = parseLocations(bn, ggep
                        .getBytes(GGEP.GGEP_HEADER_ALTS));
            } catch (BadGGEPPropertyException bad) {
            }
        }

        if (ggep.hasKey(GGEP.GGEP_HEADER_CREATE_TIME)) {
            try {
                createTime = ggep.getLong(GGEP.GGEP_HEADER_CREATE_TIME) * 1000;
            } catch (BadGGEPPropertyException bad) {
            }
        }

        if (ggep.hasKey(GGEP.GGEP_HEADER_LARGE_FILE)) {
            try {
                size64 = ggep.getLong(GGEP.GGEP_HEADER_LARGE_FILE);
            } catch (BadGGEPPropertyException bad) {
            }
        }
        
        if (ggep.hasKey(GGEP.GGEP_HEADER_TTROOT)) {
            try {
                byte []tt = ggep.get(GGEP.GGEP_HEADER_TTROOT);
                if (tt != null)
                    ttroot = URN.createTTRootFromBytes(tt);
            } catch (IOException bad){}
        }
        
        boolean verified = false;
        IntervalSet ranges = null;
        try {
            ranges = IntervalEncoder.decode(size64,ggep);
            verified = !ggep.hasKey(GGEP.GGEP_HEADER_PARTIAL_RESULT_UNVERIFIED);
        } catch (BadGGEPPropertyException ignore){}
        
        
        if (locations == null && createTime == -1 && size64 <= Integer.MAX_VALUE && 
                ranges == null & ttroot == null)
            return GGEPContainer.EMPTY;
        
        return new GGEPContainer(locations, createTime, size64, ranges, verified, ttroot);
    }

    /**
     * Returns a set of IpPorts corresponding to the IpPorts in data. If
     * BitNumbers is non-null, the addresses in the index corresponding to any
     * 'on' indexes in BitNumbers are considered tlsCapable. Whenever an invalid
     * address is encountered, all further hosts are prevented from being TLS
     * capable.
     */
    private Set<? extends IpPort> parseLocations(BitNumbers tlsHosts,
            byte[] data) {
        Set<IpPort> locations = null;

        if (data.length % 6 != 0)
            return null;

        int size = data.length / 6;
        byte[] current = new byte[6];
        for (int i = 0; i < size; i++) {
            System.arraycopy(data, i * 6, current, 0, 6);
            IpPort ipp;
            try {
                ipp = IPPortCombo.getCombo(current);
            } catch (InvalidDataException ide) {
                tlsHosts = null; // turn off TLS
                continue;
            }

            // if we're me or banned, ignore.
            if (!ipFilter.allow(ipp.getAddress()) || networkInstanceUtils.isMe(ipp))
                continue;

            if (locations == null)
                locations = new IpPortSet();

            // if this addr was TLS-capable, mark it as such.
            if (tlsHosts != null && tlsHosts.isSet(i))
                ipp = new ConnectableImpl(ipp, true);

            locations.add(ipp);
        }
        return locations;
    }

    /**
     * A container for information we're putting in/out of GGEP blocks.
     */
    static final class GGEPContainer {
        final Set<? extends IpPort> locations;
        final long createTime;
        final long size64;
        static final GGEPContainer EMPTY = new GGEPContainer();
        final IntervalSet ranges;
        final boolean verified;
        final URN ttroot;

        private GGEPContainer() {
            this(null, -1, 0, null, false, null);
        }

        GGEPContainer(Set<? extends IpPort> locs, long create, long size64, IntervalSet ranges, boolean verified, URN ttroot) {
            if (locs == null)
                locations = Collections.emptySet();
            else
                locations = Collections.unmodifiableSet(locs);
            createTime = create;
            this.size64 = size64;
            this.ranges = ranges;
            this.verified = verified;
            this.ttroot = ttroot;
            assert ttroot == null || ttroot.isTTRoot();
        }

        boolean isEmpty() {
            return locations.isEmpty() && createTime <= 0
                    && size64 <= Integer.MAX_VALUE && ranges == null && 
                    ttroot == null;
        }
    }
}
