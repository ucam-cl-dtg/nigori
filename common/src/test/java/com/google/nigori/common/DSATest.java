/*
 * Copyright (C) 2012 Daniel R. Thomas (drt24).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.nigori.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

public class DSATest {

  @Test
  public void signVerifies() throws NoSuchAlgorithmException {
    // Can have bugs which depend on particular bit values (such as sign bits) so iterate to reduce
    // probability of false positive
    for (int i = 0; i < 4; ++i) {
      Random random = new Random();
      byte[] privateKey = new byte[NigoriConstants.B_DSA];
      random.nextBytes(privateKey);
      DSASign signer = new DSASign(privateKey);
      DSASignature sig = signer.sign(MessageLibrary.toBytes("message"));
      DSAVerify verifier = new DSAVerify(signer.getPublicKey());
      assertTrue("Did not verify on iteration: " + i, verifier.verify(sig));
    }
  }

  @Test
  public void paramsPrime() {
    assertTrue(NigoriConstants.DSA_P.isProbablePrime(128));
    assertTrue(NigoriConstants.DSA_Q.isProbablePrime(256));
  }

  @Test
  public void paramGValid() {
    assertTrue(NigoriConstants.DSA_G.compareTo(BigInteger.valueOf(2)) >= 0);
    assertTrue(NigoriConstants.DSA_G.modPow(NigoriConstants.DSA_Q, NigoriConstants.DSA_P)
        .compareTo(BigInteger.valueOf(1)) == 0);
  }

  @Test
  public void paramsSize() {
    assertEquals(NigoriConstants.B_DSA * 8, NigoriConstants.DSA_P.bitLength());
    assertEquals(NigoriConstants.B_SHA256 * 8, NigoriConstants.DSA_Q.bitLength());
    assertTrue(NigoriConstants.B_DSA * 8 - 1 <= NigoriConstants.DSA_G.bitLength());
    assertTrue(NigoriConstants.DSA_G.bitLength() <= NigoriConstants.B_DSA * 8);
  }

  private static class DSATestVector {
    public final byte[] privateKey;
    public final byte[] publicKey;
    public final String message;
    public final byte[] r;
    public final byte[] s;
    public DSATestVector(byte[] privateKey, byte[] publicKey, String message, byte[] r, byte[] s){
      this.privateKey = privateKey;
      this.publicKey = publicKey;
      this.message = message;
      this.r = r;
      this.s = s;
    }
  }
  private static String message0 = "message";
  private static String message1 = "test";
  private static String message2 = "";
  private static String message3 = "A message which is longer than a short one and hence might" +
  		" trigger bugs due to long messages which are long long long long................." +
  		" _* This is a long message *_ ..........";
  private static byte[] privateKey0 = fromHex("9b78a950b30fffd08a13cf81a4aca3fca7dd663961bbbbccce8813e3948c185803386dce36c4c75bbda75a9b06cd77c5bd84c1f705ccc963b55caabaa8716dc353f9b04ba1a211e468fdd39866886d314415ccfbe7b9b9fa86469e7e0fe7740ae4b8888e01569d72bb00a5bd389f65267055eed1e9fd4a74dc80590a604c5c47618a4c6e2cf08ef05c778eda54a089ce04c42d7ff09fa94204c3ac22d5d1ed164aafbcd1387e0287e33a566377d1094adc96ec23659367e982a3161ccb68745572fd219cffacee2f5c431265c0a77fa665091adfb523957d8ccaa45c4544fd4c8295a4aea27d076ec881dae533d0f6d520fbadf0d170ca828df4d8c46ae9186d74e952d0030a93659b8aebc539c8dedd23de895baffb33cca4c43b4e03d07dc6181393644959621c401084711e06677c97f51870c742f41823678799fe922eaf927af8546044607b902d079292b107113be449ba966e6cfb100494b9edb00526daccb778766a68c85f802d7429ec38abdf8abbc61e0e3e3ba701704388dcc8c6");
  private static byte[] publicKey0 = fromHex("3374bb2303002f1c00d7dfae732c5b57318ca11ae6c92a37d8b2599ca494a1ab77b665ccb0c0ec863326b004e639025b5c9c7099bf9a389e2203b2f416d151b96cce31f03065e467eb7c1da1e227bd10e6b4d98d4abd8642d1be6c7f0fcd100cf286dac75cf0ef12145be15ccaa075792ce95154833dfffb00bf163be1f9e0bbf079450c6cb73425e265336bde631493f19a0d0094754992d6c6e89d91ac4b659bc8a5239a5f2cc4b3d790ff0eaf5f6a305236c208d3ca7e50f49dd786fa10c96da43b64b3b565870473382336e3b39e05cd017849598563d5137b4d052754934a71a2bfa0114557db5cbfaa54105528a23d44aa78a1796293eb75b51fb60d695ca7db1ed2d679d3ed78836cd55e176fb822cb2524873f807fdd4309962c0095147656aa893bbefea62842568b693f498f4e085744e73ca31b464d3a55882e684be21b98eb90dd1f48bf7490c12375f3f01f545c35cd7ff53c18f451e8b3af5b770314bffda8110269b8121c8bbde2761c23b8faac8f1b9ba484141afbf0fba7");
  private static byte[] privateKey1 = fromHex("3b423311803558b3f0134f08dba99ac0ea36371464e3174ad5164e8f27dd2562d43459a8b3ee8bbd7b1b103984190f32d520d24ff1bf9f93bf2d332148ff3a971a07cfba0be339b7ae7cfad3659d89f5ae3a50ab52710b6939168e630f158d1e545a221dc7975d4a292ace66a15394f0964ea2b698bd3a0950082339c1068dd01e957af8ea0e250f014e9243c12ece4d436d99a6d96f64cec1f6f46e48f30ed4e6b98490bc1b12723d44b154319ee3dd7fac295cad74a8da7fb116c2b2e054cb728d93c148c741c85c65a989c0c6c3e2a735d6d0b10619b531d88d00b2fc8e48590d2e4bdbc48b4acea01c6c244b14fa2da420c608eb0c29b19cb1c71e6df34c853686888bf66b5476e748dc74a425ba16b415b7d5bdcc618e55af856fb0cf17b71b922dae01e82f2f66330bac0aeafa7c405114c24eedac8be01eaf448e3515034cc1fc5ef3e2eb439010882dedb0b20d9213c47c0de9851ed5d08cdf97254e4f89882efcf49036f5f9b29d3afba8c48afd41b93486ca8df28640a1a6385956");
  private static byte[] publicKey1 = fromHex("52c54755ca2761ec02abefefc059caff830753b85e5f40f821691ccd0e0522b7fac72a95b597c75487df43401f285a13baf0c1f87d25bae89d074cb2865a45f742689d0095f8184605e874dc3544c937c72fe46261048ab80aa4e17a69ff93cd256c33279cf571259db02dff494f0d23592abfaf2a07e99b2586cf536a0b2d547d1e3dae5d407b6445dcbde4a9ecfe47df29b37f82112ee0c156ccae45600123f171c7c50bfa2bbd28b876c9b82e9d42bbc98d468abff98231a746cae88a9ae0cb9961fb23a1ac6c308b3bbe889593806b39291d80ad68f30ad996d20d03240e736450b4bccc1550193a6c3e54b880ce1cd844e6e42c48d7d5bb3176a61eb82c4141bee1f1f5d0cefbd5d9b7974ae64d8e48148d5db804ab511ac59ead24cc3e1b0ce8a7e8f3dffcf30d99c16bd8908906b9f680ac24a8a39e66a6bc4b3fe25f2e0f6ae7388c2605d2edbbcb7764067736caa1165ec0cb8e6e52814fff752454b4de5cb193484e95542429145a5fa9719e87b612e83190ca7f80ce948aa74ab0");
  private static byte[] privateKey2 = fromHex("d9ce3d71aa10fdf9f9020b4c7bbc6a1d49d26c351feef9d965d85368a3560e56aea7fefcd005d384f75001e17eb90d00b49834f3f7bf7f950add521f0583767834d59a34972ddf5d0b1a369331348a74c8a0a80779684e48294c41569dbaabb9a145bdb1716ff67c483f6f886a70695e97e078c2e50265383ac3034762b64cc9815acd678f4538883037bf605f8c06e1c62215368125dd406ed8fa51edcb2279347f42be69d1dce03ec8aeb62ee9e15c0f85135a16be352b26fdfd8afe5d73f96d0f64203a0001bf5c93f3035623c97fc14678eb71fe6c0f7930e0f4f5dcdef6b48daf8582bf22586d6e7d013c9bd5973be7b847e358bb7fb381e8e45957ef87e5295fe382413f10c48ee4e8a88d839774949480c28c014dd062e80a559ab8baaf97ca4cc351c501ab8382f445bc63ff76e91bd0e6c23b8105cbd9c536e22b7d16359cc58fb9cdbcde00c01a24a62930defc81652dba8bc538292fc0ee1a018df280146e77b44bfa0d7961483950187e5ebf903cf703acc983263f4a94b05716");
  private static byte[] publicKey2 = fromHex("5f1fb86dc01d088816b47796f736f404bafb2b1a1b12672899960b488fe931495b9ae0012c624aa28936b15582fe42dc97f90b0c1e0e9258f43149040f1484ab886b851f84aa3abf8f9c6aae178e685025a1c37cc8c27b35d494b117eb6808ed1c0ebadfa1c5c23109beb0985b817175eab10f767a7414cb5b46f04e63e7ba2d95a96d0ade0075a4e784b0eaee3f7693ae6ecfd00b4395a4dc802fd091011bb4aed22f6cda4f3fd41aa2f9e0eeaf9bfd6678b8cba9dff79c96ebf33c546f8e972c4e725e30ad06b7c37304f66690b3455c33b76099c4bbba9df1fba56062282ef7b07e889056a8399a4e960e14e37de196503eb4f1a39c95ae732fa8a6e710958c340990ea0656418a5981e22f3b28359ff1702b5e744728fb4574b8f74c472bd91bfdd9eafaa061a684eaf3eb0c4cfc17815c6b03331f927f74d63aec74fef1009013bd45fa4f9e6851da4d6a5e37c17f339a8cff75bbe7df505334070e7305e19746ecb13a459b99c6518339eac42b6ddd1fc026d3218d1141d53483b060e4");
  private static byte[] privateKey3 = fromHex("6ee1d5becb7427245d88dc4cdd9d0fbd3caada52aa50de6ab538768ce2ef70aa89baf2fe25b874ffe82e00434a39bce2d0bd03172fcca648d0043876b6f6254298d437fcdf6b377a38db797f220cacca0ad0bb5f738ae62d8c919a419134fb11c212ed529761689ad04401f6e947491ee92a4491e7032735d349657f1b7bf30f8379863fb2132cd0a316359586fefde0057b9ad901a228d743cffde19d9e842d2a85ff09d7796d799b697250c64a0227060ea394b95b0623af9c2c6985a64f725ad0da9770ed4e39130f316ed908970b2a61ecfa0d6280379e78e323e68d7e568d0a00972c1e23e25eabae38d31eaf11c3ffe5367ddc3fb5cbb76f78f593e9f58c3ef2008016e0143ea0f25d2c5549b8b7b1a4dd8fb5254aa34fc2c68d1c66f7f868186b21503e656dc7742341bffc31fef98d914c77f6a5cab8b743ac20e14dc55e12b68bd8f5c518fdf010d1b379dd28e58446c137f52096a1891ff42e274bb90354337d85cb3c5e7a31dd80550153d52772387f953f1d9270d4642bfc7bd6");
  private static byte[] publicKey3 = fromHex("39567abf687583ae41f8354d0cb2e9e1e3ddfc4e2c82e4685e16f755d9ca781d5f886a5f80f5363129c2857d0d9368f45e9e99ec838fa18f3030700a6a799010e4a10a134eabc3a27c94ea3ff2fdd38edb9a41d0046e8cf3df1278894129b4361ab98448234cac81717ce122a9abfe65a69fa0701cdf4d557244bbef6cfbffb0508bc8a5c6013c9335619abf2d98dc6b293ef9cccce09c68fb72abdc03a2da4f140adbd5c0f7627436ef988708f7c636d754ef6933cef0a2b851be772e85cbb9a98113b95ca8f72a836222dcc3b91ff04a05594bc5919052ab9bf791aee5b29f3e0016e59797402f2282c6d09381ba8f0bb0cef43580898940701001b53f7bee74f9552581bb8d4c9ad96653150518df905a06fa406c82a551db0018fd87ab8fddd2d1af0eb255a8bbaeaeeb22d8cbc531ecbef333cacded87002249b6d6f5522507d70029f31e565fae254df3aba4723637877d83cd7b6b1bc0b234bb7832c4bd48d2ef83776f028521f5e2ead12f93403ff3476d84ae668a9598b41b7903a5");
  private static byte[] privateKey4 = fromHex("f2d080457b396062d5ffd93657ab15993a8d41261cc4fcb3fd96da80bb8363fb5dab41d54b38f4fd06d07d7a4249e458a2b5aeccb0a8b18f49b3ef1032b83e6f400a8fb1788b9021ec798a8691882db9eb247bed59064ced9c833564c3c39d1eb06da917d37648877973d1a47750aadc05a1e768b7bab69d52a8141cc4d8694d1c253a2cecfc84941284baf08e6e32df77e08a10700a098a79361b792e924857c06556249d738c540eff68b8527f2593da88dfe3adfda2ac531a2b2d20e4daa733b15e3a8028b8ef3ace5d16a166d82e5b7d23589cccc8a9d84922ff5c9cb914da8bf44f74150aa1574c6068415809903d7d6226ec5b8f3231c76627c972515d7283804b1ca514f09befb7575a709cb95512c6db54c92b253e730010efa900378a8502d55f8766012cc400def8db22b68ccf84110c08f8ed11b02a0e7e3f18f606862613e995ec23a8b624118a91ee1761df0ac3518dbe0471d387bec7aca2969e75d76ea9c8fb5da0ae43886699b4e564e235b2e60c1f82586960e8413712a2");
  private static byte[] publicKey4 = fromHex("28e019c78bb27c8e9177c775bc509a35b1edc0f022ea69fa676cd754b62573a7ae68fbddbf87e634976828dad3f3b192730ebf9a8919b100814deb7e2088a37a53e78e3a082de2a227db08f5598cad298388f8fcff63db52e234e2a4da600cb35e2352c63bc2690ad0f00e580e17eb85a017444c957773eb2b0f320c0962c07616597ab8db88ceaf4e16ce78178cca2617b2fd48966082fe72a2560e98c26c3f92de37c7c280e887f0766a48f5fcc69950c7182751b2893d21a135c5cd72504103a83aa6399c7eb79a3920eef9d0a8f66cbb9f6fabe7f3066318ee1e447d6dea0192948e55db84e60a78583a62c65b5b364f5ec9c3e6590a62541066e83162d3ed6c6de1062e3903e31579dddb3066cc7953adb8bb2002db484467fab542468dc0d41e9453d93b01e9344f5aad806b4d58854b46a6d5bced016ff4cf4f243179f2d704f71dd73073329a4819e7a76461e0dbe73a852ec17feb6d4228a503d12c803a64a01b69b23772982d938f317613f2277adf9e2bd4d39349873b7c0cc236");
  
  /**
   * These test vectors were generated using {@link #testVectors()} this is a little sad as it means
   * that they came from the same implementation as we are using, however these test vectors can
   * then be used in other implementations.
   */
  private static DSATestVector[] testVectors = {
    new DSATestVector(privateKey0, publicKey0, message0,
        fromHex("727b40c7e63510addb70142f28c43ae061132055a8d500115cdf4ee7fe5572c0"), fromHex("7abd1856e5a568ea665a78f90743bf8642a687d0085c6ac0d50e7cd07c5befd2")),
    new DSATestVector(privateKey0, publicKey0, message1,
        fromHex("008a257fde3b08ba4114bc881d822416f36dbe59d1d2ab9351df8f39ba0d3a112a"),fromHex("00962ca7d37cf8281747fd480d047b85c0af608fdfdff8bc028f179ea0c02c3bda")),
    new DSATestVector(privateKey0, publicKey0, message2,
        fromHex("00989198cddbaaacc048e1791dc17026c39b9878a3a17c45b1ebb2d6a92ac14cb0"),fromHex("13d460e95a2869f23fd0dc723f0f590136dc60d2ca5b62624bac06bd9a1e9043")),
    new DSATestVector(privateKey0, publicKey0, message3,
        fromHex("2e6472861fbaf1190c8fac6863bdde2141d692696c63943b1b76e2587ca04bf8"),fromHex("7e4fdd3ae5f01658f28b360bd73cd0c583acd8cd9f9b9508c6f4ab68809464e9")),
    new DSATestVector(privateKey1, publicKey1, message0,
        fromHex("1ccc8a69846fe99d0612b60d2db854d66949dda5e7a2169fbb2980e1333b8123"),fromHex("76e1c7604d43b5f94654bdc85e2b6efbc304a69f2b124d1ede372cd76237de7e")),
    new DSATestVector(privateKey1, publicKey1, message1,
        fromHex("00a8658b22b53c8a78d862742596a3f35874b3798736354f26d7007f5d6c757778"),fromHex("68e314fef51eea7a00ab81a7e3b0f52b261a9ba847e4596793a84598110590c9")),
    new DSATestVector(privateKey1, publicKey1, message2,
        fromHex("21d46f9f37009b15ed622708cebb6a18a55d4adf501d150e4cfd0eca94ea9d2b"),fromHex("28621902f1481980f63b518ce1a4f65e32e61efa37464133c57a28ff974b411e")),
    new DSATestVector(privateKey1, publicKey1, message3,
        fromHex("00b982af22ca1e864ee7ea6d8fb6f72b14b1bd6f78071c3a1c61211980634d175d"),fromHex("7b8d086e18fc9d0daff704326918668894d31209981da5584c73e5ba271b0799")),
    new DSATestVector(privateKey2, publicKey2, message0,
        fromHex("1b876095652a552730b038c52469a88db90197c99ea00b7b7ec7662ce24ba341"),fromHex("1961e2d3a35b429c6bdaa27db81ebf2ad517a164b9c27b2d4875c0f710dcee6b")),
    new DSATestVector(privateKey3, publicKey3, message0,
        fromHex("00ac72b384fd715814417d3b344d2e9962f91e4d03a9627213d12cf9083e6f26e2"),fromHex("44e0a2827f4ead2d2057ec26304595bb658db95cfaffedba57002f7836aaa698")),
    new DSATestVector(privateKey4, publicKey4, message0,
        fromHex("52edf7c3b0f8a66e765724254a0bdbd5bb4651e2757db0121e284e2f6cf8a7ca"),fromHex("17214f43286e3ca34186866064a0f84f59a845218d8a838eae11b9868edf0508"))
  };

  @Test
  public void testVectors() throws NoSuchAlgorithmException {
    for (int i = 0; i < testVectors.length; ++i){
      DSATestVector vector = testVectors[i];
      DSASign signer = new DSASign(vector.privateKey);
      assertArrayEquals(vector.publicKey,signer.getPublicKey());
      DSASignature sig = signer.sign(MessageLibrary.toBytes(vector.message));
      assertTrue("iteration: " + i, signer.verify(sig));
      assertTrue("iteration: " + i, signer.verify(new DSASignature(vector.r, vector.s, MessageLibrary.toBytes(vector.message))));
      // Since DSA has a per signature random nonce we can't check the validity of the signature only of verification.
    }
  }

  private static byte[] fromHex(String data) {
    try {
      return Hex.decodeHex(data.toCharArray());
    } catch (DecoderException e) {
      throw new RuntimeException(e);
    }
  }
  public void generateTestVectors() throws NoSuchAlgorithmException {
    byte[] privateKey = new byte[NigoriConstants.B_DSA];
    Random random = new Random();
    for (int i = 0; i < 5; ++i) {
      random.nextBytes(privateKey);
      DSASign signer = new DSASign(privateKey);

      System.out.println("Private key: " + Hex.encodeHexString(privateKey));
      System.out.println("Public key: " + Hex.encodeHexString(signer.getPublicKey()));

      String[] messages =
          {
              "message",
              "",
              "test",
              "A message which is longer than a short one and hence might trigger bugs due to long messages which are long long long long................. _* This is a long message *_ .........."};
      for (String message : messages) {
        DSASignature sig = signer.sign(MessageLibrary.toBytes(message));
        System.out.println("Message: '" + message + "'");
        System.out.println("Signature: r: " + Hex.encodeHexString(sig.getR()) + " s: "
            + Hex.encodeHexString(sig.getS()));
      }
    }
  }
}
