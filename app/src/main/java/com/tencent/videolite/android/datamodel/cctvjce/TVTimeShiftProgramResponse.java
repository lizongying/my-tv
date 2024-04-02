package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import java.util.Collection;
public final class TVTimeShiftProgramResponse extends JceStruct {
    static ArrayList<TVProgram> cache_programs = new ArrayList<>();
    public int errcode;
    public String errmsg;
    public ArrayList<TVProgram> programs;
    public int timeShiftFlag;

    static {
        cache_programs.add(new TVProgram());
    }

    public TVTimeShiftProgramResponse() {
        this.errcode = 0;
        this.errmsg = "";
        this.programs = null;
        this.timeShiftFlag = 0;
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.errcode = jceInputStream.read(this.errcode, 0, true);
        this.errmsg = jceInputStream.readString(1, false);
        this.programs = (ArrayList) jceInputStream.read(cache_programs, 2, false);
        this.timeShiftFlag = jceInputStream.read(this.timeShiftFlag, 3, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.errcode, 0);
        String str = this.errmsg;
        if (str != null) {
            jceOutputStream.write(str, 1);
        }
        ArrayList<TVProgram> arrayList = this.programs;
        if (arrayList != null) {
            jceOutputStream.write((Collection) arrayList, 2);
        }
        jceOutputStream.write(this.timeShiftFlag, 3);
    }

    public TVTimeShiftProgramResponse(int i2, String str, ArrayList<TVProgram> arrayList, int i3) {
        this.errcode = 0;
        this.errmsg = "";
        this.programs = null;
        this.timeShiftFlag = 0;
        this.errcode = i2;
        this.errmsg = str;
        this.programs = arrayList;
        this.timeShiftFlag = i3;
    }
}
