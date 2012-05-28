/*
 * Copyright (C) 2011 Daniel Thomas (drt24)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.nigori.common;

import java.math.BigInteger;

import org.bouncycastle.crypto.params.DSAParameters;

/**
 * @see "Constants" section of RFC
 * 
 * @author drt24
 *
 */
public final class NigoriConstants {
  /**
   * Static final utility class
   */
  private NigoriConstants(){}

  //This is the constant "user salt" (in non-terminated ascii) written here as bytes
  public final static byte[] USER_SALT = {117, 115, 101, 114, 32, 115, 97, 108, 116};

/**
 * Number of rounds for PBKDF for each key generation
 */
  public static final int N_SALT = 1000;
  public static final int N_USER = N_SALT + 1;
  public static final int N_ENC = N_SALT + 2;
  public static final int N_MAC = N_SALT + 3;
  public static final int N_IV = N_SALT + 4;
  
  // Sizes in bytes
  public static final int B_AES = 16;
  public static final int B_SHA1 = 20;
  public static final int B_SHA256 = 32;
  /**
   * Size in bytes of MAC
   */
  public static final int B_MAC = B_SHA256;
  /**
   * Block size for symmetric encryption
   */
  public static final int B_SYMENC = B_AES;
  /**
   * for Suser, must be at least 10 for FIPS-198 compliance with HMAC_SHA1
   */
  public static final int B_SUSER = B_AES;
  
  /**
   * for Kuser
   */
  public static final int B_DSA = 384;//3072 bits => 128 bit security
  public static final int B_KEYHASH = B_SHA1;
  /**
   * symmetric encryption key size (AES-128) (Kenc)
   */
  public static final int B_KENC = B_AES;
  /**
   * for Kmac
   */
  public static final int B_KMAC = B_AES;
  /**
   * for Kmaster
   */
  public static final int B_KMASTER = B_AES;
  
  public static final String A_HMAC = "HmacSHA256";
  public static final String A_KMAC = "SHA-256";
  public static final String A_SYMENC = "AES";
  public static final String A_SYMENC_CIPHER = A_SYMENC + "/CBC/PKCS5Padding";
  public static final String A_SIGN = "DSA";
  public static final String A_SIG = "SHA256with" + A_SIGN;
  public static final String A_REVHASH = "SHA-1";
  public static final String A_KEYHASH = A_REVHASH;

  /** Example parameters from NIST as listed on https://www.cl.cam.ac.uk/~drt24/crypto/ */
  public static final BigInteger DSA_P = new BigInteger("90066455B5CFC38F9CAA4A48B4281F292C260FEEF01FD61037E56258A7795A1C7AD46076982CE6BB956936C6AB4DCFE05E6784586940CA544B9B2140E1EB523F009D20A7E7880E4E5BFA690F1B9004A27811CD9904AF70420EEFD6EA11EF7DA129F58835FF56B89FAA637BC9AC2EFAAB903402229F491D8D3485261CD068699B6BA58A1DDBBEF6DB51E8FE34E8A78E542D7BA351C21EA8D8F1D29F5D5D15939487E27F4416B0CA632C59EFD1B1EB66511A5A0FBF615B766C5862D0BD8A3FE7A0E0DA0FB2FE1FCB19E8F9996A8EA0FCCDE538175238FC8B0EE6F29AF7F642773EBE8CD5402415A01451A840476B2FCEB0E388D30D4B376C37FE401C2A2C2F941DAD179C540C1C8CE030D460C4D983BE9AB0B20F69144C1AE13F9383EA1C08504FB0BF321503EFE43488310DD8DC77EC5B8349B8BFE97C2C560EA878DE87C11E3D597F1FEA742D73EEC7F37BE43949EF1A0D15C3F3E3FC0A8335617055AC91328EC22B50FC15B941D3D1624CD88BC25F3E941FDDC6200689581BFEC416B4B2CB73",16);
  public static final BigInteger DSA_Q = new BigInteger("CFA0478A54717B08CE64805B76E5B14249A77A4838469DF7F7DC987EFCCFB11D",16);
  public static final BigInteger DSA_G = new BigInteger("5E5CBA992E0A680D885EB903AEA78E4A45A469103D448EDE3B7ACCC54D521E37F84A4BDD5B06B0970CC2D2BBB715F7B82846F9A0C393914C792E6A923E2117AB805276A975AADB5261D91673EA9AAFFEECBFA6183DFCB5D3B7332AA19275AFA1F8EC0B60FB6F66CC23AE4870791D5982AAD1AA9485FD8F4A60126FEB2CF05DB8A7F0F09B3397F3937F2E90B9E5B9C9B6EFEF642BC48351C46FB171B9BFA9EF17A961CE96C7E7A7CC3D3D03DFAD1078BA21DA425198F07D2481622BCE45969D9C4D6063D72AB7A0F08B2F49A7CC6AF335E08C4720E31476B67299E231F8BD90B39AC3AE3BE0C6B6CACEF8289A2E2873D58E51E029CAFBD55E6841489AB66B5B4B9BA6E2F784660896AFF387D92844CCB8B69475496DE19DA2E58259B090489AC8E62363CDF82CFD8EF2A427ABCD65750B506F56DDE3B988567A88126B914D7828E2B63A6D7ED0747EC59E0E0A23CE7D8A74C1D2C2A7AFB6A29799620F00E11C33787F7DED3B30E1A22D09F1FBDA1ABBBFBF25CAE05A13F812E34563F99410E73B",16);
  public static final DSAParameters DSA_PARAMS = new DSAParameters(DSA_P, DSA_Q, DSA_G);
}
