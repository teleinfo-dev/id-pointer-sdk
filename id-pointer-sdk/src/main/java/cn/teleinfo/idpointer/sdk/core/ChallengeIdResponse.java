/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import java.security.SecureRandom;
import java.util.Random;

/***********************************************************************
 * Object used to represent a challenge sent to a user asking for
 * proof of their identity.  The challenge includes a nonce (a random
 * unpredictable set of bytes) as well as a digest of the request that
 * is being challenged (so that the user can verify that they are only
 * authorizing a specific operation).
 ***********************************************************************/
public class ChallengeIdResponse extends AbstractIdResponse {
    private static Random random = null;
    private static final String randomLock = "randomLock";

    public byte nonce[];

    /**********************************************************************
     * Construct a challenge to the specified request.  This
     * constructor is used on the client side, when decoding messages.
     **********************************************************************/
    public ChallengeIdResponse(int opCode, byte nonce[]) {
        super(opCode, AbstractMessage.RC_AUTHENTICATION_NEEDED);
        this.nonce = nonce;
    }

    /**********************************************************************
     * Construct a challenge to the specified request.  This
     * constructor is used on the server side.
     **********************************************************************/
    public ChallengeIdResponse(AbstractIdRequest req) throws HandleException {
        this(req, false);
    }

    /**********************************************************************
     * Construct a challenge to the specified request.
     * The compatibility parameter should be set to true when this
     * challenge response is being created artificially to check a
     * secret key.
     **********************************************************************/
    public ChallengeIdResponse(AbstractIdRequest req, boolean compatibility) throws HandleException {
        super(req, AbstractMessage.RC_AUTHENTICATION_NEEDED);

        // if the request didn't ask for a request digest, we'll compute one anyway.
        // because they are essential for security of the challenge/response exchange.
        // request digests are normally computed in the AbstractResponse constructor.
        if (requestDigest == null) {
            takeDigestOfRequest(req);
        }

        this.returnRequestDigest = true;
        this.nonce = generateNonce();

        if (compatibility) {
            this.suggestMajorProtocolVersion = Common.COMPATIBILITY_MAJOR_VERSION;
            this.suggestMinorProtocolVersion = Common.COMPATIBILITY_MINOR_VERSION;
        }
    }

    public static final void initializeRandom() {
        initializeRandom(null);
    }

    public static final void initializeRandom(byte seed[]) {
        if (random == null) {
            synchronized (randomLock) {
                if (random == null) {
                    if (seed == null) {
                        random = new SecureRandom();
                        random.setSeed(System.nanoTime());
                    } else random = new SecureRandom(seed);
                    random.nextInt();
                }
            }
        }
    }

    private static Random getRandom() {
        if (random == null) initializeRandom();
        return random;
    }

    /**********************************************************************
     * Generate a nonce.
     **********************************************************************/
    public static byte[] generateNonce() {
        // generate a nonce to make this a unique non-repeatable challenge
        byte[] nonce = new byte[Common.CHALLENGE_NONCE_SIZE];
        getRandom().nextBytes(nonce);
        return nonce;
    }

}
