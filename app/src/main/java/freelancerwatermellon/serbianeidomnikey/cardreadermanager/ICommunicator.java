package freelancerwatermellon.serbianeidomnikey.cardreadermanager;

import com.freelancewatermelon.licnakarta.cardreadermanager.ccid.CcidDescriptor;

import java.nio.ByteBuffer;

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
