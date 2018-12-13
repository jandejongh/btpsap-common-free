/*
 * Copyright 2016-2018 Jan de Jongh, TNO.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package net.etsi.btpsap.client.udptno;

import java.util.logging.Logger;
import net.etsi.btpsap.BtpSapTypes;
import net.etsi.btpsap.BtpSap_DataIndContainer;

/** Formatting and parsing UDP-TNO BtpSap Indication PDUs.
 * 
 * @author Jan de Jongh, TNO.
 * 
 * @see BtpSap_DataIndContainer
 * 
 */
public class UdpTnoDataInd
{

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // LOGGING
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG = Logger.getLogger (UdpTnoDataInd.class.getName ());
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS / FACTORIES / CLONING
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /** Inhibits instantiation.
   * 
   */
  private UdpTnoDataInd ()
  {
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // MAGIC BYTES
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private final static byte MAGIC_1 = (byte) (Integer.parseInt ("3d", 16) & 0xff);
  private final static byte MAGIC_2 = (byte) (Integer.parseInt ("94", 16) & 0xff);
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // FORMAT INDICATION
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public final static byte [] formatIndication (final BtpSap_DataIndContainer indication, final int clientID)
  {
    if (indication == null)
      return null;
    if (clientID < 0 || clientID > 127)
      return null;
    final int length = indication.getLength ();
    final int unpaddedSize = 80 + length;
    // XXX Clumsy way of doing this...
    int paddedSize = unpaddedSize;
    while (paddedSize % 4 != 0)
      paddedSize++;
    final int size = paddedSize;
    final byte[] pdu = new byte[size];
    pdu[0] = MAGIC_1;
    pdu[1] = MAGIC_2;
    pdu[2] = (byte) 1;
    pdu[3] = (byte) clientID;
    pdu[4] = (byte) 0; // Units...
    pdu[5] = (byte) 0;
    pdu[6] = (byte) 0; // XXX Btp Flags...
    pdu[7] = (byte) 0; // XXX Gn SubType
    final Integer btpSrcPort = indication.getBtpSrcPort ();
    if (btpSrcPort != null)
    {
      pdu[8] = (byte) ((btpSrcPort & 0xff00) >> 8);
      pdu[9] = (byte) (btpSrcPort & 0x00ff);
    }
    else
    {
      pdu[8] = (byte) 0;
      pdu[9] = (byte) 0;      
    }
    pdu[10] = (byte) 0; // XXX Rem Lifetime...
    if (indication.getGnTrafficClass () != null && (indication.getGnTrafficClass () instanceof BtpSapTypes.DefaultGnTrafficClass))
      pdu[11] = ((BtpSapTypes.DefaultGnTrafficClass) indication.getGnTrafficClass ()).getTrafficClassByte ();
    else
      pdu[11] = (byte) 0; // We do not know...
    final int btpDstPort = indication.getBtpDstPort ();
    pdu[12] = (byte) ((btpDstPort & 0xff00) >> 8);
    pdu[13] = (byte) (btpDstPort & 0x00ff);
    final Integer btpDstPortInfo = indication.getBtpDstPortInfo ();
    if (btpDstPortInfo != null)
    {
      pdu[14] = (byte) ((btpDstPortInfo & 0xff00) >> 8);
      pdu[15] = (byte) (btpDstPortInfo & 0x00ff);
    }
    else
    {
      pdu[14] = (byte) 0;
      pdu[15] = (byte) 0;      
    }
    // XXX Dst Latitude OR DstGnUc
    pdu[16] = (byte) 0;
    pdu[17] = (byte) 0;
    pdu[18] = (byte) 0;
    pdu[19] = (byte) 0;
    pdu[20] = (byte) 0;
    pdu[21] = (byte) 0;
    pdu[22] = (byte) 0;
    pdu[23] = (byte) 0;
    // XXX Dst Longitude
    pdu[24] = (byte) 0;
    pdu[25] = (byte) 0;
    pdu[26] = (byte) 0;
    pdu[27] = (byte) 0;
    pdu[28] = (byte) 0;
    pdu[29] = (byte) 0;
    pdu[30] = (byte) 0;
    pdu[31] = (byte) 0;
    // XXX DST Distance A
    pdu[32] = (byte) 0;
    pdu[33] = (byte) 0;
    // XXX DST Distance B
    pdu[34] = (byte) 0;
    pdu[35] = (byte) 0;
    // XXX DST Angle
    pdu[36] = (byte) 0;
    pdu[37] = (byte) 0;
    // RESERVED_1
    pdu[38] = (byte) 0;
    pdu[39] = (byte) 0;
    // XXX SRC_GN_ADDRESS
    pdu[40] = (byte) 0;
    pdu[41] = (byte) 0;
    pdu[42] = (byte) 0;
    pdu[43] = (byte) 0;
    pdu[44] = (byte) 0;
    pdu[45] = (byte) 0;
    pdu[46] = (byte) 0;
    pdu[47] = (byte) 0;
    final BtpSapTypes.GnPositionVector gnSrcPV = indication.getGnSrcPV ();
    // SRC_LATITUDE
    if (gnSrcPV != null)
    {
      final long latBits = Double.doubleToLongBits (indication.getGnSrcPV ().getLatitude ());
      pdu[48] = (byte) ((latBits >> 56) & 0xff);
      pdu[49] = (byte) ((latBits >> 48) & 0xff);
      pdu[50] = (byte) ((latBits >> 40) & 0xff);
      pdu[51] = (byte) ((latBits >> 32) & 0xff);
      pdu[52] = (byte) ((latBits >> 24) & 0xff);
      pdu[53] = (byte) ((latBits >> 16) & 0xff);
      pdu[54] = (byte) ((latBits >>  8) & 0xff);
      pdu[55] = (byte) ((latBits      ) & 0xff);      
    }
    else
    {
      pdu[48] = (byte) 0;
      pdu[49] = (byte) 0;
      pdu[50] = (byte) 0;
      pdu[51] = (byte) 0;
      pdu[52] = (byte) 0;
      pdu[53] = (byte) 0;
      pdu[54] = (byte) 0;
      pdu[55] = (byte) 0;
    }
    // SRC_LONGITUDE
    if (gnSrcPV != null)
    {
      final long lonBits = Double.doubleToLongBits (indication.getGnSrcPV ().getLongitude ());
      pdu[56] = (byte) ((lonBits >> 56) & 0xff);
      pdu[57] = (byte) ((lonBits >> 48) & 0xff);
      pdu[58] = (byte) ((lonBits >> 40) & 0xff);
      pdu[59] = (byte) ((lonBits >> 32) & 0xff);
      pdu[60] = (byte) ((lonBits >> 24) & 0xff);
      pdu[61] = (byte) ((lonBits >> 16) & 0xff);
      pdu[62] = (byte) ((lonBits >>  8) & 0xff);
      pdu[63] = (byte) ((lonBits      ) & 0xff);      
    }
    else
    {
      pdu[56] = (byte) 0;
      pdu[57] = (byte) 0;
      pdu[58] = (byte) 0;
      pdu[59] = (byte) 0;
      pdu[60] = (byte) 0;
      pdu[61] = (byte) 0;
      pdu[62] = (byte) 0;
      pdu[63] = (byte) 0;
    }
    // SECURITY REPORT LENGTH
    // [+ SECURITY REPORT]
    pdu[64] = (byte) 0;
    pdu[65] = (byte) 0;
    pdu[66] = (byte) 0;
    pdu[67] = (byte) 0;
    // CERTIFICATE ID LENGTH
    // [+ CERTIFICATE ID]
    pdu[68] = (byte) 0;
    pdu[69] = (byte) 0;
    pdu[70] = (byte) 0;
    pdu[71] = (byte) 0;
    // PERMISSIONS LENGTH
    // [+ PERMISSIONS]
    pdu[72] = (byte) 0;
    pdu[73] = (byte) 0;
    pdu[74] = (byte) 0;
    pdu[75] = (byte) 0;
    // PAYLOAD
    pdu[76] = (byte) ((length & 0xff000000) >>> 24);
    pdu[77] = (byte) ((length & 0x00ff0000) >>> 16);
    pdu[78] = (byte) ((length & 0x0000ff00) >>> 8);
    pdu[79] = (byte) (length & 0x000000ff);
    final byte[] data_src = indication.getData ();
    final int offset_src = indication.getOffset ();
    System.arraycopy (data_src, offset_src, pdu, 80, length);
    // 32-bit padding.
    for (int i = 80 + length; i < size; i++)
      pdu[i] = (byte) 0;
    return pdu;
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // PARSE INDICATION
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public final static BtpSap_DataIndContainer parseIndication
  ( final byte[] pdu,
    final int offset,
    final int length)
  {
    if (pdu == null || offset < 0 || length < 0 || offset + length > pdu.length)
      return null;
    if (length < 80)
      return null;
    // 0, 1: Magic.
    if (pdu[offset+0] != MAGIC_1 || pdu[offset+1] != MAGIC_2)
      return null;
    // 2: Unknown... Always unity? Version? XXX
    //if (pdu[offset+2] != (byte) 1)
    //  return null;
    // 3: Client ID. XXX Report back?? XXX int??
    final byte clientID = pdu[offset+3];
    // 4, 5: Units?? XXX Report back? Always zero?
    // 6 XXX Btp Flags...
    // 7 XXX Gn SubType
    // 8, 9 btpSrcPort; 0 -> no btpSrcPort provided? XXX
    final Integer btpSrcPort = ((((int) pdu[offset+8]) & 0xff) << 8) + (((int) pdu[offset+9]) & 0xff);
    // 10: XXX Rem Lifetime...
    final Integer gnRemLifetime_s = 1;
    // 11: gnTrafficClass
    final BtpSapTypes.GnTrafficClass gnTrafficClass = new BtpSapTypes.DefaultGnTrafficClass (pdu[offset+11]);
    // 12, 13 btpDstPort; 0 -> no btpDstPort provided? XXX
    final int btpDstPort = ((((int) pdu[offset+12]) & 0xff) << 8) + (((int) pdu[offset+13]) & 0xff);
    // 14, 15 btpDstPortInfo; 0 -> no btpDstPortInfo provided? XXX
    final Integer btpDstPortInfo = ((((int) pdu[offset+14]) & 0xff) << 8) + (((int) pdu[offset+15]) & 0xff);
    // 16 - 23: XXX Dst Latitude OR DstGnUc
    // 24 - 31: XXX XXX Dst Longitude
    // 32, 33: XXX DST Distance A
    // 34, 35: XXX DST Distance B
    // 36, 37: XXX DST Angle
    final BtpSapTypes.GnDestination gnDstAddress = null;
    // 38, 39: RESERVED_1
    // 40 - 47: XXX SRC_GN_ADDRESS
    // 48 - 55: SRC_LATITUDE
    // 56 - 63: SRC_LONGITUDE
    final BtpSapTypes.GnPositionVector gnSrcPV = null;
    // 64 - 67: SECURITY REPORT LENGTH [+ SECURITY REPORT]
    // Must be all-zero in current implementation...
    // XXX This should be easy to fix...
    if ( pdu[offset+64] != (byte) 0
      || pdu[offset+65] != (byte) 0
      || pdu[offset+66] != (byte) 0
      || pdu[offset+67] != (byte) 0)
      return null;
    final BtpSapTypes.GnSecurityReport gnSecReport = null;
    // 68 - 71: CERTIFICATE ID LENGTH [+ CERTIFICATE ID]
    // Must be all-zero in current implementation...
    // XXX This should be easy to fix...
    if ( pdu[offset+68] != (byte) 0
      || pdu[offset+69] != (byte) 0
      || pdu[offset+70] != (byte) 0
      || pdu[offset+71] != (byte) 0)
      return null;
    final BtpSapTypes.GnCertificateId gnCertId = null;
    // 72 - 75: PERMISSIONS LENGTH [+ PERMISSIONS]
    // Must be all-zero in current implementation...
    // XXX This should be easy to fix...
    if ( pdu[offset+72] != (byte) 0
      || pdu[offset+73] != (byte) 0
      || pdu[offset+74] != (byte) 0
      || pdu[offset+75] != (byte) 0)
      return null;
    final BtpSapTypes.GnPermissions gnPermissions = null;
    // 76 - 79: PAYLOAD LENGTH
    final int payLoadLength =
        ((((int) pdu[offset+76]) & 0xff) << 24)
      + ((((int) pdu[offset+77]) & 0xff) << 16)
      + ((((int) pdu[offset+78]) & 0xff) <<  8)
      + ((((int) pdu[offset+79]) & 0xff));
    if (payLoadLength < 0)
      return null;
    final int payLoadAndPaddingLength = (payLoadLength % 4 == 0) ? payLoadLength : ((payLoadLength / 4) + 1) * 4;
    if (payLoadAndPaddingLength + 80 /* XXX */ != length)
     return null;
    // 80 - ...: PAYLOAD
    final byte[] payload = new byte[payLoadLength];
    final int payloadOffset = 0;
    System.arraycopy (pdu, offset + 80, payload, payloadOffset, payLoadLength);
    return new BtpSap_DataIndContainer
      ( btpSrcPort,
        btpDstPort,
        btpDstPortInfo,
        gnDstAddress,
        gnSrcPV,
        gnSecReport,
        gnCertId,
        gnPermissions,
        gnTrafficClass,
        gnRemLifetime_s,
        payLoadLength,
        payloadOffset,
        payload);
  }
  
}
