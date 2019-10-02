/*
 *  MusicTag Copyright (C)2003,2004
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 *  or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 *  you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jaudiotagger.tag.id3.framebody;

import org.jaudiotagger.tag.InvalidTagException;
import org.jaudiotagger.tag.id3.ID3v24Frames;

import java.nio.ByteBuffer;

/**
 * Official internet radio station homepage URL link frames.
 * <p>The 'Official internet radio station homepage' contains a URL pointing at the homepage of the internet radio station.
 * <p/>
 * <p>For more details, please refer to the ID3 specifications:
 * <ul>
 * <li><a href="http://www.id3.org/id3v2.3.0.txt">ID3 v2.3.0 Spec</a>
 * </ul>
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id: FrameBodyWORS.java,v 1.12 2008/01/01 15:14:20 paultaylor Exp $
 */
public class FrameBodyWORS extends AbstractFrameBodyUrlLink implements ID3v24FrameBody, ID3v23FrameBody
{
    /**
     * Creates a new FrameBodyWORS datatype.
     */
    public FrameBodyWORS()
    {
    }

    /**
     * Creates a new FrameBodyWORS datatype.
     *
     * @param urlLink
     */
    public FrameBodyWORS(String urlLink)
    {
        super(urlLink);
    }

    public FrameBodyWORS(FrameBodyWORS body)
    {
        super(body);
    }

    /**
     * Creates a new FrameBodyWORS datatype.
     *
     * @throws InvalidTagException
     */
    public FrameBodyWORS(ByteBuffer byteBuffer, int frameSize)
            throws InvalidTagException
    {
        super(byteBuffer, frameSize);
    }

    /**
     * The ID3v2 frame identifier
     *
     * @return the ID3v2 frame identifier  for this frame type
     */
    public String getIdentifier()
    {
        return ID3v24Frames.FRAME_ID_URL_OFFICIAL_RADIO;
    }
}