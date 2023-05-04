/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package GuiPack;

import java.util.ArrayList;

public class DataBeanMaker {

    public ArrayList<DataBean> getDataBeanList() {
        ArrayList<DataBean> dataBeanList = new ArrayList<DataBean>();
        return dataBeanList;
    }

    public static DataBean produce(String CustomerName, String Address, String BillDate, String Amount, String duration) {
        DataBean dataBean = new DataBean();
        dataBean.setTitle(CustomerName);
        dataBean.setTitle(Address);
        dataBean.setTitle(BillDate);
        dataBean.setTitle(Amount);
        dataBean.setTitle(duration);
        return dataBean;
    }
}
