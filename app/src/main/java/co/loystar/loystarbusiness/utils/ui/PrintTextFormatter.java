package co.loystar.loystarbusiness.utils.ui;

/**
 * Created by ordgen on 11/29/17.
 */

public class PrintTextFormatter {
    /** The format that is being build on */
    private byte[] mFormat;

    public PrintTextFormatter() {
        // Default:
        mFormat = new byte[]{27, 33, 0};
    }

    public byte[] get() {
        return mFormat;
    }

    public byte[] bold() {
        // Apply bold:
        mFormat[2] = ((byte) (0x8 | mFormat[2]));
        return mFormat;
    }

    public byte[] small() {
        mFormat[2] = ((byte) (0x1 | mFormat[2]));
        return mFormat;
    }

    public byte[] underlined() {
        mFormat[2] = ((byte) (0x80 | mFormat[2]));
        return mFormat;
    }

    public byte[] rightAlign(){
        return new byte[]{0x1B, 'a', 0x02};
    }

    public byte[] leftAlign(){
        return new byte[]{0x1B, 'a', 0x00};
    }

    public byte[] centerAlign(){
        return new byte[]{0x1B, 'a', 0x01};
    }
}
