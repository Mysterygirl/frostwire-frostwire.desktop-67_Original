
package de.kapsi.net.daap.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Locale;

import de.kapsi.net.daap.chunks.Chunk;

public class ContentCodesGenerator {
    
    public static final String CLASS = "ContentCodesResponseImpl";
    public static final String FILE = ChunkUtil.CHUNK_DIR + "/" + CLASS + ".java";
    
    public static final String CLASS_COMMENT 
        = "/**\n"
        + " * This class is machine-made by {" + ContentCodesGenerator.class.getName() + "}!\n"
        + " * It is needed because Reflection cannot list the classes of a package so that we\n"
        + " * must pre-create a such list manually. This file must be rebuild whenever a class\n"
        + " * is removed or a class is added to the {@see de.kapsi.net.daap.chunks.impl} package.\n"
        + " */";
    
    public static void main(String[] args) throws Exception {
        StringBuffer buffer = new StringBuffer();
        buffer.append(CLASS_COMMENT);
        buffer.append("\n");
        
        buffer.append("package ").append(ChunkUtil.CHUNK_PACKAGE).append(";\n");
        buffer.append("\n");
        
        buffer.append("import ").append(ChunkUtil.CHUNK_IMPL_PACKAGE).append(".Status;\n");
        buffer.append("import ").append(ChunkUtil.CHUNK_IMPL_PACKAGE).append(".ContentCodesResponse;\n");
        buffer.append("\n");
        
        buffer.append("public final class ").append(CLASS).append(" extends ContentCodesResponse {\n");
        buffer.append("    public ").append(CLASS).append("() {\n");
        buffer.append("        super();\n");
        buffer.append("        add(new Status(200));\n");
                
        Chunk[] chunks = ChunkUtil.getChunks();
        
        for (int i = 0; i < chunks.length; i++) {
            Chunk chunk = chunks[i];
            String contentCode = "0x" + Integer.toHexString(chunk.getContentCode()).toUpperCase(Locale.US);
            String contentCodeString = chunk.getContentCodeString();
            String name = chunk.getName();
            int type = chunk.getType();

            buffer.append("        ");
            buffer.append("add(new ContentCode(").append(contentCode).append(", \"").append(name).append("\", ").append(type).append(")); //").append(contentCodeString);
            buffer.append("\n");
        }
            
        buffer.append("    }\n");
        buffer.append("}\n");
        
        System.out.println(buffer);
        
        BufferedWriter out = new BufferedWriter(new FileWriter(new File(FILE)));
        //Writer out = new OutputStreamWriter(new FileOutputStream(FILE), DaapUtil.UTF_8);
        out.write(buffer.toString());
        out.close();
    }
}
