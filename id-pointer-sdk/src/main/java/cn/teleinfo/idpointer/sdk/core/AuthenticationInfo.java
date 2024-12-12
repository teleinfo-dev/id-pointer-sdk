/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

public abstract class AuthenticationInfo {

    /***********************************************************************
     * Get the identifier for the type of authentication performed.  This
     * authentication type needs to be the same type as the handle value
     * that the user's handle/id refers to.
     ***********************************************************************/
    public abstract byte[] getAuthType();

    /***********************************************************************
     * Sign the given nonce and requestDigest given as a challenge to the
     * given request.  The implementation of this method should also probably
     * verify that the client did in fact send the specified request, and
     * that the associated digest is a valid digest of the request.
     * @return a signature of the concatenation of nonce and requestDigest.
     ***********************************************************************/
    public abstract byte[] authenticate(ChallengeIdResponse challenge, AbstractIdRequest request) throws HandleException;

    /***********************************************************************
     * Get the name of the handle that identifies the user that is
     * represented by this authentication object.
     ***********************************************************************/
    public abstract byte[] getUserIdHandle();

    /***********************************************************************
     * Get the index of the handle value that identifies this user.
     * The returned index value of the handle that identifies this user
     * should contain a value with a type (public key, secret key, etc)
     * that corresponds to the way that this user is authenticating.
     ***********************************************************************/
    public abstract int getUserIdIndex();

    /***********************************************************************
     * Get the ValueReference that identifies the user that is
     * represented by this authentication object.
     ***********************************************************************/
    public ValueReference getUserValueReference() {
        return new ValueReference(getUserIdHandle(), getUserIdIndex());
    }

    /***********************************************************************
     * Construct an authentication object from the given string.
     * The string should specify the handle and index that identifies the
     * entity being authenticated.  The string should also specify either
     * the secret key or the public and private keys.
     * The authentication specification string for the user with
     * secret key XXXX at handle cnri/sr (index 11) should look
     * something like this:
     * <pre>
     *   secretkey:11:cnri/sr:&lt;secret-key&gt;
     * </pre>
     ***********************************************************************/
    /*
    public static AuthenticationInfo getInstance(String authStr)
    throws HandleException
    {
    String authType = StringUtils.fieldIndex(authStr,':',0);
    if(authType.equals("secretkey")) {
      String idIdxStr = StringUtils.fieldIndex(authStr,':',1);
      String idHdlStr = StringUtils.fieldIndex(authStr,':',2);
      String secKey = StringUtils.fieldIndex(authStr,':',3);
      int idIdx = 0;
      try {
        idIdx = Integer.parseInt(idIdxStr.trim());
      } catch (Exception e) {
        throw new HandleException(HandleException.INVALID_VALUE,
                                  "Invalid admin ID index value: "+idIdxStr);
      }
      return new
        SecretKeyAuthenticationInfo(Util.encodeString(idHdlStr), idIdx,
                                    Util.encodeString(secKey));
    
    } else if(authType.equals("privatekey")) {
      String idIdxStr = StringUtils.fieldIndex(authStr,':',1);
      String idHdlStr = StringUtils.fieldIndex(authStr,':',2);
      String privKeyStr = StringUtils.fieldIndex(authStr,':',3);
      int idIdx = 0;
      try {
        idIdx = Integer.parseInt(idIdxStr.trim());
      } catch (Exception e) {
        throw new HandleException(HandleException.INVALID_VALUE,
                                  "Invalid admin ID index value: "+idIdxStr);
      }
    
      byte passphrase[] = null;
      byte privKeyBytes[] = null;
      try {
        passphrase = Util.getPassphrase();
        privKeyBytes = Util.decrypt(Util.encodeHexString(privKeyStr), passphrase);
      } catch (Exception e) {
        throw new HandleException(HandleException.INVALID_VALUE,
                                  "Error decrypting private key: "+e);
      } finally {
        for(int i=0; i<passphrase.length; i++) passphrase[i] = (byte)0;
      }
    
      AuthenticationInfo info = null;
      try {
        info = new
          PublicKeyAuthenticationInfo(Util.encodeString(idHdlStr), idIdx,
                                      Util.getPrivateKeyFromBytes(privKeyBytes,0));
      } catch (Exception e) {
        throw new HandleException(HandleException.INVALID_VALUE,
                                  "Error initializing private key: "+e);
      } finally {
        for(int i=0; i<privKeyBytes.length; i++) privKeyBytes[i] = (byte)0;
      }
      return info;
    } else {
      throw new HandleException(HandleException.INVALID_VALUE,
                                "Unknown authentication type: \""+authType+'"');
    }
    }
     */
}
