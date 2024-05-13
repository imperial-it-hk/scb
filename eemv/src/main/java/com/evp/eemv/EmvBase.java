/*
 * ===========================================================================================
 * = COPYRIGHT
 *          PAX Computer Technology(Shenzhen) CO., LTD PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or nondisclosure
 *   agreement with PAX Computer Technology(Shenzhen) CO., LTD and may not be copied or
 *   disclosed except in accordance with the terms in that agreement.
 *     Copyright (C) 2019-? PAX Computer Technology(Shenzhen) CO., LTD All rights reserved.
 * Description: // Detail description about the function of this module,
 *             // interfaces with the other modules, and dependencies.
 * Revision History:
 * Date                  Author	                 Action
 * 20190108  	         Linhb                   Create
 * ===========================================================================================
 */

package com.evp.eemv;

import android.util.SparseArray;

import com.evp.eemv.entity.AidParam;
import com.evp.eemv.entity.CTransResult;
import com.evp.eemv.entity.Capk;
import com.evp.eemv.entity.Config;
import com.evp.eemv.entity.InputParam;
import com.evp.eemv.exception.EEmvExceptions;
import com.evp.eemv.exception.EmvException;

import java.util.ArrayList;
import java.util.List;

public abstract class EmvBase implements IEmv {
    protected List<Capk> capkList;
    protected List<AidParam> aidParamList;
    protected Config cfg = new Config();
    private SparseArray<byte[]> tags = new SparseArray<>();

    static {
        System.loadLibrary("F_DEVICE_LIB_PayDroid");
        System.loadLibrary("F_PUBLIC_LIB_PayDroid");
    }

    protected EmvBase() {
        capkList = new ArrayList<>();
        aidParamList = new ArrayList<>();
    }

    @Override
    public IEmv getEmv() {
        return this;
    }

    @Override
    public void init() throws EmvException {
        tags.clear();
    }

    @Override
    public final byte[] getTlv(int tag) {
        if (tag == 0x71 || tag == 0x72) {
            return tags.get(tag);
        }
        return getTlvSub(tag);
    }

    @Override
    public final void setTlv(int tag, byte[] value) throws EmvException {
        if (tag == 0x71 || tag == 0x72) {
            tags.put(tag, value);
            return;
        }
        setTlvSub(tag, value);
    }

    @Override
    public void setConfig(Config emvCfg) {
        cfg = emvCfg;
    }

    @Override
    public Config getConfig() {
        return cfg;
    }

    // Run callback
    // Parameter settings, loading aid,
    // select the application, application initialization,read application data, offline data authentication,
    // terminal risk management,cardholder authentication, terminal behavior analysis,
    // issuing bank data authentication, execution script
    @Override
    public CTransResult process(InputParam inputParam) throws EmvException {
        throw new EmvException(EEmvExceptions.EMV_ERR_NO_KERNEL);
    }

    @Override
    public void setListener(IEmvListener listener) {
        //do nothing
    }

    @Override
    public void setAidParamList(List<AidParam> aidParamList) {
        this.aidParamList = aidParamList == null ? new ArrayList<AidParam>() : aidParamList;
    }

    @Override
    public void setCapkList(List<Capk> capkList) {
        this.capkList = capkList == null ? new ArrayList<Capk>() : capkList;
    }

    @Override
    public String getVersion() {
        return "";
    }

    protected abstract byte[] getTlvSub(int tag);

    protected abstract void setTlvSub(int tag, byte[] value) throws EmvException;
}