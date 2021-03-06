package com.limegroup.gnutella.gui.options.panes;

import java.io.File;

import com.limegroup.gnutella.gui.search.NamedMediaType;
import com.limegroup.gnutella.gui.tables.BasicDataLineModel;

/**
 * The table model.
 */
class MediaTypeDownloadDirModel extends BasicDataLineModel<MediaTypeDownloadDirDataLine, NamedMediaType> {

	/**
	 * @param dataLineClass
	 */
	public MediaTypeDownloadDirModel() {
		super(MediaTypeDownloadDirDataLine.class);
	}
	
	public MediaTypeDownloadDirDataLine createDataLine() {
	    return new MediaTypeDownloadDirDataLine();
	}

	public boolean isCellEditable(int row, int col) {
		return col == 1;
	}

	public void setValueAt(Object value, int row, int col) {
        get(row).setDirectory(new File((String)value));
	}
}