package freelancerwatermellon.serbianeidomnikey.cardreadermanager;

import java.nio.ByteBuffer;

import freelancerwatermellon.serbianeidomnikey.cardreadermanager.ccid.CcidDescriptor;

public interface ICommunicator {
    boolean bulkIn(ByteBuffer byteBuffer, int i);

    boolean bulkOut(byte[] bArr);

    CcidDescriptor getCcidDescriptor();

    int getId();

    String getMacAddress();

    String getName();

    int getProductId();

    int getType();

    boolean initialize();

    void shutdown();
}
