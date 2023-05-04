/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GuiPack;

import java.awt.FileDialog;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.view.JasperViewer;

public class MainFrm extends javax.swing.JFrame {

    /**
     * Creates new form WelcomeFrm
     */
    ByteArrayOutputStream bos;
    BufferedImage input_img = null;
    Double footConst = 0.3048;
    Double inchesConst = 0.0254;
    byte[] memberphoto = null;
    Connection con;
    Statement stmt;
    public String connection = "jdbc:mysql://localhost:3306/Gymdb";
    public String user = "root";
    public String password = "pass";
    DefaultTableModel tm, tm1, tm2, tm3, tm4;
    Object[] colHeader, colHeader1, colHeader2, colHeader3, colHeader4;
    String[] row;
    String[] duration = new String[4];
    String[] gender = new String[2];
    ArrayList<Integer> allEnquiryID;
    boolean memberSearched = false;
    boolean memberSearchedPayment = false;
    Timer timer;
    GenerateBillReport timerTask1;
    String searchedMemberName = "";
    String SearchedMemberAddress = "";
    String SearchedMemberDuration = "";

    public MainFrm() {
        initComponents();
        this.setLocationRelativeTo(null);
        colHeader = new Object[]{"Sr.No", "Name", "Contact", "Gender", "Address", "Duration", "Enquiry Date"};
        tm = new DefaultTableModel(colHeader, 0);
        colHeader1 = new Object[]{"Sr.No", "Name", "Contact", "Gender", "Present Date"};
        tm1 = new DefaultTableModel(colHeader1, 0);
        colHeader2 = new Object[]{"Sr.No", "Name", "Contact", "Gender", "Address", "Duration", "Subscription Start", "Subscription End"};
        tm2 = new DefaultTableModel(colHeader2, 0);
        colHeader3 = new Object[]{"Sr.No", "Name", "Contact", "Gender", "Duration", "Subscription Start", "Subscription End", "Total Fees", "Fees Paid", "Fees Pending"};
        tm3 = new DefaultTableModel(colHeader3, 0);
        colHeader4 = new Object[]{"Sr.No", "Payment Date", "Amount"};
        tm4 = new DefaultTableModel(colHeader4, 0);
        duration[0] = "1 Month";
        duration[1] = "3 Month";
        duration[2] = "6 Month";
        duration[3] = "1 Year";
        gender[0] = "Male";
        gender[1] = "FeMale";
        txtSubscriptionStatus.setVisible(false);
        Display_Subscription_End_Count();
    }

    public void initdatabase() {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            con = DriverManager.getConnection(connection, user, password);
            //SSSystem.out.println("Database Connection OK");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error in Database connection: " + e);
            System.out.println("Error opening database : " + e);
        }
    }

    void Load_Member_Photo() {
        try {
            FileDialog fd = new FileDialog(this, "Select Member Photo", FileDialog.LOAD);
            fd.setVisible(true);
            if (fd.getDirectory() == null) {
                return;
            }
            String file_path = fd.getDirectory() + fd.getFile();
            System.out.println(file_path);
            input_img = null;
            input_img = ImageIO.read(new FileInputStream(file_path));
            ImageIcon i1 = new ImageIcon(input_img);
            File image = new File(file_path);
            FileInputStream fis = new FileInputStream(image);
            byte[] buff = new byte[1024];
            bos = new ByteArrayOutputStream();
            for (int readNum; (readNum = fis.read(buff)) != -1;) {
                bos.write(buff, 0, readNum);
            }
            memberphoto = new byte[1024];
            memberphoto = bos.toByteArray();
            lblPhoto.setIcon(i1);

        } catch (Exception e) {
            System.out.println("Error: " + e);
            e.printStackTrace();
        }
    }

    void delete_Member() {
        try {
            if (memberSearched) {
                initdatabase();
                int id = Integer.parseInt(txtMemberId.getText());
                stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                String qry = "delete from memberdetails where memberId=" + id;
                int rs = stmt.executeUpdate(qry);
                int resp = 0;
                if (rs > 0) {
                    stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    qry = "delete from MemberAttendance where memberId=" + id;
                    resp = stmt.executeUpdate(qry);
                    if (resp > 0) {
                        JOptionPane.showMessageDialog(this, "Member Deleted");
                        clear_All_Fields();
                    } else {
                        JOptionPane.showMessageDialog(this, "Something went wrong");
                    }
                }
                con.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void insert_Member_fees() {
        try {
            if (jTextField4.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter installment amount");
                return;
            }
            initdatabase();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            String toDate = dateFormat.format(date);
            double instalment = Double.parseDouble(jTextField4.getText());
            int id = Integer.parseInt(jTextField5.getText());
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String qry = "insert into MemberPaymentLog values(null," + id + ",'" + toDate + "'," + instalment + ")";
            int rs = stmt.executeUpdate(qry);
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            qry = "select feespaid from memberdetails where memberId=" + id;
            ResultSet res = stmt.executeQuery(qry);
            double oldFeePaid = 0;
            if (res.next()) {
                oldFeePaid = res.getDouble(1);
            }
            double newFees = oldFeePaid + instalment;
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            qry = "update memberdetails set feespaid=" + newFees + " where memberId=" + id;
            int rs1 = stmt.executeUpdate(qry);
            if (rs1 > 0) {
                JOptionPane.showMessageDialog(this, "Fees Updated");
            } else {
                JOptionPane.showMessageDialog(this, "Something Went Wrong");
            }
            con.close();
            search_Member_Payment();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void search_Member_Payment() {
        try {
            if (jTextField5.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter ID to Search");
                return;
            }
           // jTextField4.setText("");
            searchedMemberName = "";
            SearchedMemberAddress = "";
            SearchedMemberDuration = "";
            initdatabase();
            int id = Integer.parseInt(jTextField5.getText());
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String qry = "select * from memberdetails where memberId=" + id;
            ResultSet res = stmt.executeQuery(qry);
            if (res.next()) {
                memberSearchedPayment = true;
                searchedMemberName = res.getString(2);
                SearchedMemberAddress = res.getString(10);
                SearchedMemberDuration = duration[res.getInt(9)];
                jTextField1.setText("" + res.getDouble(11));
                jTextField2.setText("" + res.getDouble(12));
                double feesPending = res.getDouble(11) - res.getDouble(12);
                jTextField6.setText("" + feesPending);
            }

            //colHeader4 = new Object[]{"Sr.No", "Payment Date", "Amount"};
            tm4 = new DefaultTableModel(colHeader4, 0);
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            qry = "select * from MemberPaymentLog where memberId=" + id;
            ResultSet res1 = stmt.executeQuery(qry);
            int cnt = 1;
            while (res1.next()) {
                row = new String[3];
                row[0] = "" + cnt;
                row[1] = res1.getString(3);
                row[2] = "" + res1.getDouble(4);
                tm4.addRow(row);
                cnt++;
            }
            jTable5.setModel(tm4);
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void search_Member() {
        try {
            if (txtMemberId.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter ID to Search");
                return;
            }
            memberSearched = false;
            initdatabase();
            int id = Integer.parseInt(txtMemberId.getText());
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String qry = "select * from memberdetails where memberId=" + id;
            ResultSet res = stmt.executeQuery(qry);
            if (res.next()) {
                memberSearched = true;
                txtMemberName.setText(res.getString(2));
                txtMemberContact.setText(res.getString(3));
                txtAge.setText(res.getString(4));
                txtHeight.setText(res.getString(5));
                txtWeight.setText(res.getString(6));
                txtBmi.setText(res.getString(7));
                jComboBox3.setSelectedIndex(res.getInt(8));
                jComboBox4.setSelectedIndex(res.getInt(9));
                txtAddress.setText(res.getString(10));
                txtTotalFees.setText("" + res.getDouble(11));
                txtSubscriptionStartDate.setText(res.getString(13));
                txtSubscriptionEndDate.setText(res.getString(14));
                calculate_Subscription_RemaningDays(res.getString(14));
                memberphoto = res.getBytes(15);
                if (memberphoto == null) {
                    lblPhoto.setIcon(null);
                } else {
                    lblPhoto.setIcon(new ImageIcon(memberphoto));
                }

            } else {
                JOptionPane.showMessageDialog(this, "No Record Found");
            }
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void delete_Enquiry() {
        try {
            initdatabase();
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String qry = "delete from Enquirydetails where EnquiryId=" + allEnquiryID.get(jTable2.getSelectedRow());
            int rs = stmt.executeUpdate(qry);
            if (rs > 0) {
                JOptionPane.showMessageDialog(this, "Enquiry Deleted");
            } else {
                JOptionPane.showMessageDialog(this, "Something Went Wrong");
            }
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void Load_Fees_Pending_Log() {
        try {
            //colHeader3 = new Object[]{"Sr.No", "Name", "Contact", "Gender", "Duration", "Subscription Start", "Subscription End", "Total Fees", "Fees Paid", "Fees Pending"};
            tm3 = new DefaultTableModel(colHeader3, 0);
            initdatabase();
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String query = "select * from memberdetails where feespaid < totalfees";
            ResultSet rs = stmt.executeQuery(query);
            int cnt = 1;
            while (rs.next()) {
                row = new String[10];
                row[0] = "" + cnt;
                row[1] = rs.getString(2);
                row[2] = rs.getString(3);
                row[3] = gender[rs.getInt(8)];
                row[4] = duration[rs.getInt(9)];
                row[5] = rs.getString(13);
                row[6] = rs.getString(14);
                row[7] = "" + rs.getDouble(11);
                row[8] = "" + rs.getDouble(12);
                double difference = rs.getDouble(11) - rs.getDouble(12);
                row[9] = "" + difference;
                tm3.addRow(row);
                cnt++;
            }
            jTable3.setModel(tm3);
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void Load_Customer_Log() {
        try {
            tm2 = new DefaultTableModel(colHeader2, 0);
            initdatabase();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            String toDate = dateFormat.format(date);
            //   colHeader2 = new Object[]{"Sr.No", "Name", "Contact", "Gender", "Address", "Duration", "Subscription Start Date", "Subscription End Date"};
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String query = "select * from memberdetails";
            ResultSet rs = stmt.executeQuery(query);
            int cnt = 1;
            while (rs.next()) {
                row = new String[8];
                row[0] = "" + cnt;
                row[1] = rs.getString(2);
                row[2] = rs.getString(3);
                row[3] = gender[rs.getInt(8)];
                row[4] = rs.getString(10);
                row[5] = duration[rs.getInt(9)];
                row[6] = rs.getString(13);
                row[7] = rs.getString(14);
                tm2.addRow(row);
                cnt++;

            }
            jTable3.setModel(tm2);
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void Display_Subscription_End_Count() {
        try {

            initdatabase();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            String toDate = dateFormat.format(date);
            //   colHeader2 = new Object[]{"Sr.No", "Name", "Contact", "Gender", "Address", "Duration", "Subscription Start Date", "Subscription End Date"};
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String query = "select * from memberdetails where subscriptionEndDate<='" + toDate + "'";
            ResultSet rs = stmt.executeQuery(query);
            int cnt = 0;
            while (rs.next()) {
                cnt++;
            }

            lblSubscriptionEndDisplay.setText("<html>Subscription Ending<br><center>(" + cnt + ")</center></html>");
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void Load_Subscription_End_Log() {
        try {
            tm2 = new DefaultTableModel(colHeader2, 0);
            initdatabase();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            String toDate = dateFormat.format(date);
            //   colHeader2 = new Object[]{"Sr.No", "Name", "Contact", "Gender", "Address", "Duration", "Subscription Start Date", "Subscription End Date"};
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String query = "select * from memberdetails where subscriptionEndDate<='" + toDate + "'";
            ResultSet rs = stmt.executeQuery(query);
            int cnt = 1;
            while (rs.next()) {
                row = new String[8];
                row[0] = "" + cnt;
                row[1] = rs.getString(2);
                row[2] = rs.getString(3);
                row[3] = gender[rs.getInt(8)];
                row[4] = rs.getString(10);
                row[5] = duration[rs.getInt(9)];
                row[6] = rs.getString(13);
                row[7] = rs.getString(14);
                tm2.addRow(row);
                cnt++;

            }
            jTable3.setModel(tm2);
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void Load_Attedance_Log() {
        try {
            if (jComboBox6.getSelectedIndex() == 0) {
                jLabel9.setText("Member Wise Attendance Log");
                if (jTextField3.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Enter Member Id");
                    return;
                }
                tm1 = new DefaultTableModel(colHeader1, 0);
                initdatabase();
                int id = Integer.parseInt(jTextField3.getText());
                stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                String query = "select * from memberdetails where memberId=" + id;
                ResultSet rs = stmt.executeQuery(query);
                String memberName = "";
                String memberContact = "";
                String membergender = "";
                if (rs.next()) {
                    memberName = rs.getString(2);
                    memberContact = rs.getString(3);
                    membergender = gender[rs.getInt(8)];
                }
                stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                String qry = "select * from MemberAttendance where memberId=" + id;
                ResultSet res = stmt.executeQuery(qry);
                int cnt = 1;

                while (res.next()) {
                    row = new String[5];
                    row[0] = "" + cnt;
                    row[1] = memberName;
                    row[2] = memberContact;
                    row[3] = membergender;
                    row[4] = res.getString(3);
                    tm1.addRow(row);
                    cnt++;
                }
                con.close();
                jTable4.setModel(tm1);
            } else {
                jLabel9.setText("Day Wise Attendance Log");
                tm1 = new DefaultTableModel(colHeader1, 0);
                initdatabase();
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = new Date();
                String toDate = dateFormat.format(date);
                stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                String query = "select * from MemberAttendance where AttendanceDate='" + toDate + "'";
                ResultSet rs = stmt.executeQuery(query);
                int cnt = 1;
                while (rs.next()) {

                    int memberId = rs.getInt(2);
                    stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    query = "select * from memberdetails where memberId=" + memberId;
                    ResultSet res = stmt.executeQuery(query);

                    if (res.next()) {
                        row = new String[5];
                        row[0] = "" + cnt;
                        row[1] = res.getString(2);
                        row[2] = res.getString(3);
                        row[3] = gender[res.getInt(8)];
                        row[4] = toDate;
                        tm1.addRow(row);

                    }
                    cnt++;
                }
                con.close();
                jTable4.setModel(tm1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void Load_Enquiry() {
        try {
            tm = new DefaultTableModel(colHeader, 0);
            allEnquiryID = new ArrayList();
            initdatabase();
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String qry = "select * from Enquirydetails";
            ResultSet res = stmt.executeQuery(qry);
            int cnt = 1;
            while (res.next()) {
                row = new String[7];
                allEnquiryID.add(res.getInt(1));
                row[0] = "" + cnt;
                row[1] = res.getString(2);
                row[2] = res.getString(3);
                row[3] = gender[res.getInt(4)];
                row[4] = res.getString(6);
                row[5] = duration[res.getInt(5)];
                row[6] = res.getString(7);
                tm.addRow(row);
                cnt++;
            }
            jTable2.setModel(tm);
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void add_Enquiry() {
        try {
            if (txtEnquiryMemName.getText().isEmpty() || txtEnquiryContact.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "All Fields are MAndatory");
                return;
            }

            String memberName = txtEnquiryMemName.getText().trim();
            String membercontact = txtEnquiryContact.getText().trim();
            int Gender = jComboBox1.getSelectedIndex();
            int duration = jComboBox2.getSelectedIndex();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            String toDate = dateFormat.format(date);
            initdatabase();
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String qry = "insert into Enquirydetails values(null,'" + memberName + "','" + membercontact + "'," + Gender + "," + duration + ",'" + jTextArea1.getText() + "','" + toDate + "')";
            int rs = stmt.executeUpdate(qry);
            if (rs > 0) {
                JOptionPane.showMessageDialog(this, "Enquiry Added");
            } else {
                JOptionPane.showMessageDialog(this, "Something Went Wrong");
            }
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void mark_Member_Attendance() {
        try {
            if (txtMemberId.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "No Member Selected");
                return;
            }
            initdatabase();
            boolean flag = false;
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            String toDate = dateFormat.format(date);
            int id = Integer.parseInt(txtMemberId.getText());
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String query = "select * from MemberAttendance where memberId=" + id + " AND AttendanceDate='" + toDate + "'";
            ResultSet res = stmt.executeQuery(query);
            if (res.next()) {
                flag = true;
            }
            if (!flag) {
                stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                String qry = "insert into MemberAttendance values(null," + id + ",'" + toDate + "')";
                int rs = stmt.executeUpdate(qry);
                if (rs > 0) {
                    JOptionPane.showMessageDialog(this, "Attendance Marked");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Attendance Already Marked for member");
            }
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void add_Member() {
        try {

            if (txtMemberName.getText().isEmpty() || txtMemberContact.getText().isEmpty() || txtAge.getText().isEmpty() || txtAddress.getText().isEmpty() || txtTotalFees.getText().isEmpty() || txtSubscriptionStartDate.getText().isEmpty() || txtSubscriptionEndDate.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "All Fields are MAndatory");
                return;
            }

            String memberName = txtMemberName.getText().trim();
            String membercontact = txtMemberContact.getText().trim();
            String memberAge = txtAge.getText().trim();
            String memberHeight = txtHeight.getText().trim();
            String memberWeight = txtWeight.getText().trim();
            String memberBMI = txtBmi.getText().trim();
            int Gender = jComboBox3.getSelectedIndex();
            int duration = jComboBox4.getSelectedIndex();
            String Address = txtAddress.getText().trim();
            double totalFees = Double.parseDouble(txtTotalFees.getText());
            double feesPaid = 0;
            String startDate = txtSubscriptionStartDate.getText();
            String endDate = txtSubscriptionEndDate.getText();
            initdatabase();
            PreparedStatement prs;
            String qry = "insert into memberdetails (Membername,contact,age,height,weight,bmi,gender,duration,address,totalfees,feesPaid,subscriptionStartDate,subscriptionEndDate,memberPhoto) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
            prs = con.prepareStatement(qry);
            prs.setString(1, memberName);
            prs.setString(2, membercontact);
            prs.setString(3, memberAge);
            prs.setString(4, memberHeight);
            prs.setString(5, memberWeight);
            prs.setString(6, memberBMI);
            prs.setInt(7, Gender);
            prs.setInt(8, duration);
            prs.setString(9, Address);
            prs.setDouble(10, totalFees);
            prs.setDouble(11, feesPaid);
            prs.setString(12, startDate);
            prs.setString(13, endDate);
            prs.setBytes(14, memberphoto);
            prs.execute();
            JOptionPane.showMessageDialog(this, "Member Added Successfully");
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Something went Wrong");
        }
    }

    void remove_Member_Photo() {
        try {
            if (memberSearched) {
                memberphoto = null;
                initdatabase();
                PreparedStatement prs;
                lblPhoto.setIcon(null);
                String qry = "update memberdetails set memberPhoto=? where memberId=?";
                prs = con.prepareStatement(qry);
                prs.setBytes(1, memberphoto);
                prs.setInt(2, Integer.parseInt(txtMemberId.getText()));
                prs.execute();
                JOptionPane.showMessageDialog(this, "Member Photo Updated Successfully");
                con.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void update_Member() {
        try {

            if (txtMemberName.getText().isEmpty() || txtMemberContact.getText().isEmpty() || txtAge.getText().isEmpty() || txtAddress.getText().isEmpty() || txtTotalFees.getText().isEmpty() || txtSubscriptionStartDate.getText().isEmpty() || txtSubscriptionEndDate.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "All Fields are Mandatory");
                return;
            }

            if (memberSearched) {
                String memberName = txtMemberName.getText().trim();
                String membercontact = txtMemberContact.getText().trim();
                String memberAge = txtAge.getText().trim();
                String memberHeight = txtHeight.getText().trim();
                String memberWeight = txtWeight.getText().trim();
                String memberBMI = txtBmi.getText().trim();
                int Gender = jComboBox3.getSelectedIndex();
                int duration = jComboBox4.getSelectedIndex();
                String Address = txtAddress.getText().trim();
                double totalFees = Double.parseDouble(txtTotalFees.getText());

                String startDate = txtSubscriptionStartDate.getText();
                String endDate = txtSubscriptionEndDate.getText();
                initdatabase();
                PreparedStatement prs;
                String qry = "update memberdetails set Membername=?,contact=?,age=?,height=?,weight=?,bmi=?,gender=?,duration=?,address=?,totalfees=?,subscriptionStartDate=?,subscriptionEndDate=?,memberPhoto=? where memberId=?";
                prs = con.prepareStatement(qry);
                prs.setString(1, memberName);
                prs.setString(2, membercontact);
                prs.setString(3, memberAge);
                prs.setString(4, memberHeight);
                prs.setString(5, memberWeight);
                prs.setString(6, memberBMI);
                prs.setInt(7, Gender);
                prs.setInt(8, duration);
                prs.setString(9, Address);
                prs.setDouble(10, totalFees);
                prs.setString(11, startDate);
                prs.setString(12, endDate);
                prs.setBytes(13, memberphoto);
                prs.setInt(14, Integer.parseInt(txtMemberId.getText()));
                prs.execute();
                JOptionPane.showMessageDialog(this, "Member Details Updated Successfully");
                con.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Something went Wrong");
        }
    }

    void find_nextDate(int increment) {
        try {
            Date current = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String toDate = dateFormat.format(current);
            txtSubscriptionStartDate.setText(toDate);
            System.out.println(toDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(current);
            cal.set(Calendar.MONTH, (cal.get(Calendar.MONTH) + increment));
            current = cal.getTime();
            toDate = dateFormat.format(current);
            txtSubscriptionEndDate.setText(toDate);
            //System.out.println(toDate);
        } catch (Exception e) {
        }
    }

    void calculate_Subscription_RemaningDays(String endDate) {
        try {
            txtSubscriptionStatus.setVisible(true);
            Date current = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String toDate = dateFormat.format(current);
            //String date1 = "2019-03-05";
            //String date2 = "2019-03-10";
            String format = "yyyy-MM-dd";
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            Date dateObj1 = sdf.parse(toDate);
            Date dateObj2 = sdf.parse(endDate);
            long diff = dateObj2.getTime() - dateObj1.getTime();
            int diffDays = (int) (diff / (24 * 60 * 60 * 1000));
            //System.out.println("difference between days: " + diffDays);
            if (diffDays < 0) {
                txtSubscriptionStatus.setText("Subscription Ended");
            } else {
                txtSubscriptionStatus.setText("Subscription will Expire in: " + diffDays + " Days");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void clear_All_Fields() {
        try {
            txtMemberId.setText("");
            txtMemberName.setText("");
            txtMemberContact.setText("");
            txtAge.setText("");
            txtHeight.setText("");
            txtWeight.setText("");
            txtBmi.setText("");
            jComboBox3.setSelectedIndex(-1);
            jComboBox4.setSelectedIndex(-1);
            txtAddress.setText("");
            txtTotalFees.setText("");

            txtSubscriptionStartDate.setText("");
            txtSubscriptionEndDate.setText("");
            txtSubscriptionStatus.setVisible(false);

            lblPhoto.setIcon(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void calculate_BMI() {
        try {
            String[] str = txtHeight.getText().toString().split("\\.");
            if (str.length > 1) {
                Double foot = Double.parseDouble(str[0]);
                Double inches = Double.parseDouble(str[1]);
                Double finalHeightInMeters = ((foot * footConst) + (inches * inchesConst));
                Double weight = Double.parseDouble(txtWeight.getText().toString());
                Double heightSquare = finalHeightInMeters * finalHeightInMeters;
                Double BMI = weight / heightSquare;
                txtBmi.setText(new DecimalFormat("##.##").format(BMI));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void change_Password() {
        try {
            if (new String(jPasswordField1.getPassword()).equals("") || new String(jPasswordField2.getPassword()).equals("") || new String(jPasswordField3.getPassword()).equals("")) {
                JOptionPane.showMessageDialog(this, "All Fields are mandatory");
                return;
            }

            String oldPassword = new String(jPasswordField1.getPassword());
            initdatabase();
            boolean flag = false;
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String qry = "select * from admindetails where password='" + oldPassword + "'";
            ResultSet rs = stmt.executeQuery(qry);
            if (rs.next()) {
                flag = true;
            }

            if (flag) {
                String newPassword = new String(jPasswordField2.getPassword());
                String newPassword2 = new String(jPasswordField3.getPassword());
                if (newPassword.equals(newPassword2)) {
                    stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    qry = "update admindetails set password='" + newPassword + "'";
                    int res = stmt.executeUpdate(qry);
                    if (res > 0) {
                        JOptionPane.showMessageDialog(this, "Password Changed");
                        this.setVisible(false);
                        new LoginFrm().setVisible(true);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Password Entry does not match");
                    return;
                }

            }
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class GenerateBillReport extends TimerTask {

        @Override
        public void run() {
            try {
                ArrayList<DataBean> dataBeanList = new ArrayList<DataBean>();
                DataBean singleBean = new DataBean();
                dataBeanList.add(DataBeanMaker.produce("", "", "", "", ""));

                String reportPath = System.getProperty("user.dir") + "\\src\\MyReport\\report1.jrxml";
                InputStream inputStream;
                inputStream = new FileInputStream(reportPath);

                dataBeanList.remove(dataBeanList.size() - 1);
                JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(dataBeanList);
                Map parameters = new HashMap();
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = new Date();
                String toDate = dateFormat.format(date);

                parameters.put("CustomerName", searchedMemberName);
                parameters.put("Address", SearchedMemberAddress);
                parameters.put("BillDate", toDate);
                parameters.put("duration", SearchedMemberDuration);
                parameters.put("Amount", "Rs." + jTextField4.getText() + " /-");

                JasperDesign jasperDesign = JRXmlLoader.load(inputStream);
                JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

                // JasperViewer.setDefaultLookAndFeelDecorated(false);
                JasperViewer.viewReport(jasperPrint, false);
                jTextField4.setText("");
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel6 = new javax.swing.JPanel();
        jLabel22 = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        txtMemberName = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        txtAge = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        txtHeight = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        txtMemberContact = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox();
        jLabel20 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        txtAddress = new javax.swing.JTextArea();
        jLabel21 = new javax.swing.JLabel();
        jComboBox4 = new javax.swing.JComboBox();
        jPanel13 = new javax.swing.JPanel();
        jButton5 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton15 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        jPanel14 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        lblPhoto = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        txtWeight = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        txtBmi = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        txtMemberId = new javax.swing.JTextField();
        jLabel26 = new javax.swing.JLabel();
        txtTotalFees = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        txtSubscriptionStartDate = new javax.swing.JTextField();
        jLabel28 = new javax.swing.JLabel();
        txtSubscriptionEndDate = new javax.swing.JTextField();
        jButton10 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        txtSubscriptionStatus = new javax.swing.JLabel();
        jButton9 = new javax.swing.JButton();
        jButton19 = new javax.swing.JButton();
        lblSubscriptionEndDisplay = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        txtEnquiryMemName = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        txtEnquiryContact = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel12 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel13 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox();
        jPanel11 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jButton12 = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        jLabel30 = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTable3 = new javax.swing.JTable();
        jLabel32 = new javax.swing.JLabel();
        jComboBox5 = new javax.swing.JComboBox();
        jButton6 = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jLabel31 = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jButton7 = new javax.swing.JButton();
        jScrollPane7 = new javax.swing.JScrollPane();
        jTable4 = new javax.swing.JTable();
        jTextField3 = new javax.swing.JTextField();
        jLabel34 = new javax.swing.JLabel();
        jComboBox6 = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        jPanel15 = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        jPanel18 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        jButton16 = new javax.swing.JButton();
        jButton17 = new javax.swing.JButton();
        jLabel37 = new javax.swing.JLabel();
        jButton18 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jLabel38 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jLabel36 = new javax.swing.JLabel();
        jPanel19 = new javax.swing.JPanel();
        jScrollPane8 = new javax.swing.JScrollPane();
        jTable5 = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPasswordField1 = new javax.swing.JPasswordField();
        jLabel2 = new javax.swing.JLabel();
        jPasswordField2 = new javax.swing.JPasswordField();
        jLabel3 = new javax.swing.JLabel();
        jPasswordField3 = new javax.swing.JPasswordField();
        jButton1 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTable1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(11, 174, 209));

        jTabbedPane1.setBackground(new java.awt.Color(255, 204, 0));
        jTabbedPane1.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N

        jPanel6.setBackground(new java.awt.Color(11, 174, 209));

        jLabel22.setBackground(new java.awt.Color(54, 94, 157));
        jLabel22.setFont(new java.awt.Font("Aparajita", 1, 36)); // NOI18N
        jLabel22.setForeground(new java.awt.Color(255, 255, 255));
        jLabel22.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel22.setText("Fitness & Health Organization");
        jLabel22.setOpaque(true);

        jPanel12.setBackground(new java.awt.Color(45, 51, 62));
        jPanel12.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 204, 204)));

        jLabel14.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(255, 255, 255));
        jLabel14.setText("Member Full Name");

        jLabel16.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(255, 255, 255));
        jLabel16.setText("Age");

        jLabel17.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel17.setForeground(new java.awt.Color(255, 255, 255));
        jLabel17.setText("Height");

        jLabel18.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(255, 255, 255));
        jLabel18.setText("Contact");

        jLabel19.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel19.setForeground(new java.awt.Color(255, 255, 255));
        jLabel19.setText("Gender");

        jComboBox3.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Male", "Female" }));

        jLabel20.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel20.setForeground(new java.awt.Color(255, 255, 255));
        jLabel20.setText("Address");

        txtAddress.setColumns(20);
        txtAddress.setRows(5);
        jScrollPane4.setViewportView(txtAddress);

        jLabel21.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel21.setForeground(new java.awt.Color(255, 255, 255));
        jLabel21.setText("Duration");

        jComboBox4.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1 Month", "3 Month", "6 Month", "1 Year" }));
        jComboBox4.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBox4ItemStateChanged(evt);
            }
        });

        jPanel13.setBackground(new java.awt.Color(45, 51, 62));
        jPanel13.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        jButton5.setBackground(new java.awt.Color(255, 204, 0));
        jButton5.setText("Add");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton8.setBackground(new java.awt.Color(255, 204, 0));
        jButton8.setText("Update");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jButton15.setBackground(new java.awt.Color(255, 204, 0));
        jButton15.setText("Delete ");
        jButton15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton15ActionPerformed(evt);
            }
        });

        jButton13.setBackground(new java.awt.Color(151, 213, 76));
        jButton13.setText("Clear All Fields");
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });

        jButton14.setBackground(new java.awt.Color(255, 51, 0));
        jButton14.setText("Exit");
        jButton14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton14ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton8, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton15, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton13)
                .addGap(18, 18, 18)
                .addComponent(jButton14, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel14.setPreferredSize(new java.awt.Dimension(200, 200));

        jScrollPane5.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jScrollPane5.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        lblPhoto.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                lblPhotoMouseReleased(evt);
            }
        });
        jScrollPane5.setViewportView(lblPhoto);

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane5)
                .addContainerGap())
        );

        jLabel23.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel23.setForeground(new java.awt.Color(255, 255, 255));
        jLabel23.setText("Weight");

        jLabel24.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel24.setForeground(new java.awt.Color(255, 255, 255));
        jLabel24.setText("BMI");

        jLabel25.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel25.setForeground(new java.awt.Color(255, 255, 255));
        jLabel25.setText("Member ID");

        txtMemberId.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMemberIdActionPerformed(evt);
            }
        });

        jLabel26.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel26.setForeground(new java.awt.Color(255, 255, 255));
        jLabel26.setText("Total Fees");

        jLabel27.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel27.setForeground(new java.awt.Color(255, 255, 255));
        jLabel27.setText("Subscription Start Date");

        jLabel28.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel28.setForeground(new java.awt.Color(255, 255, 255));
        jLabel28.setText("Subscription End Date");

        jButton10.setBackground(new java.awt.Color(151, 213, 76));
        jButton10.setText("Search");
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });

        jButton11.setBackground(new java.awt.Color(151, 213, 76));
        jButton11.setText("Obtain BMI");
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });

        txtSubscriptionStatus.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        txtSubscriptionStatus.setForeground(new java.awt.Color(255, 51, 0));
        txtSubscriptionStatus.setText("Subscription Status");

        jButton9.setBackground(new java.awt.Color(151, 213, 76));
        jButton9.setText("Mark Present");
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        jButton19.setBackground(new java.awt.Color(151, 213, 76));
        jButton19.setText("Remove Photo");
        jButton19.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton19ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel12Layout.createSequentialGroup()
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel14)
                            .addComponent(jLabel18)
                            .addComponent(jLabel16)
                            .addComponent(jLabel17)
                            .addComponent(jLabel23)
                            .addComponent(jLabel24)
                            .addComponent(jLabel19)
                            .addComponent(jLabel25))
                        .addGap(25, 25, 25)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(txtMemberName, javax.swing.GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE)
                                .addComponent(txtMemberContact)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel12Layout.createSequentialGroup()
                                    .addComponent(txtMemberId, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(jButton10, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(txtHeight, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtWeight, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel12Layout.createSequentialGroup()
                                .addComponent(txtBmi, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jButton11, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtAge, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel12Layout.createSequentialGroup()
                                .addGap(49, 49, 49)
                                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel12Layout.createSequentialGroup()
                                        .addComponent(txtSubscriptionStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGap(105, 105, 105))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel12Layout.createSequentialGroup()
                                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jPanel12Layout.createSequentialGroup()
                                                .addComponent(jLabel26)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(txtTotalFees, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(78, 78, 78))
                                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel12Layout.createSequentialGroup()
                                                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                    .addGroup(jPanel12Layout.createSequentialGroup()
                                                        .addComponent(jLabel27)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 42, Short.MAX_VALUE)
                                                        .addComponent(txtSubscriptionStartDate, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                    .addGroup(jPanel12Layout.createSequentialGroup()
                                                        .addComponent(jLabel28)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                            .addComponent(jButton9)
                                                            .addComponent(txtSubscriptionEndDate, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                                .addGap(44, 44, 44)))
                                        .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(jPanel12Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton19, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel12Layout.createSequentialGroup()
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel21)
                            .addComponent(jLabel20))
                        .addGap(98, 98, 98)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel12Layout.createSequentialGroup()
                                .addComponent(jComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel12Layout.createSequentialGroup()
                                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 243, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel12Layout.createSequentialGroup()
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel25)
                            .addComponent(txtMemberId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel26)
                            .addComponent(txtTotalFees, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel12Layout.createSequentialGroup()
                                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel14)
                                    .addComponent(txtMemberName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel18)
                                    .addComponent(txtMemberContact, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanel12Layout.createSequentialGroup()
                                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel27)
                                    .addComponent(txtSubscriptionStartDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel28)
                                    .addComponent(txtSubscriptionEndDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel16)
                            .addComponent(txtAge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton9))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel17)
                            .addComponent(txtHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel23)
                            .addComponent(txtWeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE))
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel12Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel24)
                            .addComponent(txtBmi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton11))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel19)
                            .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtSubscriptionStatus)))
                    .addGroup(jPanel12Layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(jButton19)))
                .addGap(6, 6, 6)
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(jComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel20)
                    .addComponent(jScrollPane4)
                    .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lblSubscriptionEndDisplay.setBackground(new java.awt.Color(204, 51, 0));
        lblSubscriptionEndDisplay.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lblSubscriptionEndDisplay.setForeground(new java.awt.Color(255, 255, 255));
        lblSubscriptionEndDisplay.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSubscriptionEndDisplay.setText("Subscription Ending");
        lblSubscriptionEndDisplay.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblSubscriptionEndDisplay.setOpaque(true);
        lblSubscriptionEndDisplay.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblSubscriptionEndDisplayMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 894, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblSubscriptionEndDisplay, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblSubscriptionEndDisplay, javax.swing.GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
                    .addComponent(jLabel22, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Manage Gym Members", jPanel6);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jPanel5.setBackground(new java.awt.Color(11, 174, 209));
        jPanel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel5.setBackground(new java.awt.Color(54, 94, 157));
        jLabel5.setFont(new java.awt.Font("Aparajita", 1, 36)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText(" Fitness & Health Organization");
        jLabel5.setOpaque(true);

        jPanel10.setBackground(new java.awt.Color(45, 51, 62));
        jPanel10.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 204, 204)));

        jLabel6.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("Member Full Name");

        txtEnquiryMemName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtEnquiryMemNameActionPerformed(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Contact");

        jLabel11.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(255, 255, 255));
        jLabel11.setText("Gender");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Male", "Female" }));

        jLabel12.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("Address");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        jLabel13.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("Duration");

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1 Month", "3 Month", "6 Month", "1 Year" }));

        jPanel11.setBackground(new java.awt.Color(45, 51, 62));
        jPanel11.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        jButton2.setBackground(new java.awt.Color(255, 204, 0));
        jButton2.setText("Add Enquiry");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setBackground(new java.awt.Color(255, 204, 0));
        jButton3.setText("Delete Enquiry");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setBackground(new java.awt.Color(255, 204, 0));
        jButton4.setText("Clear All");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 70, Short.MAX_VALUE)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel11Layout.createSequentialGroup()
                .addContainerGap(21, Short.MAX_VALUE)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel11Layout.createSequentialGroup()
                        .addComponent(jButton3)
                        .addGap(18, 18, 18)
                        .addComponent(jButton4))
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jLabel12))
                .addGap(18, 18, 18)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addComponent(txtEnquiryMemName, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel7)
                        .addGap(18, 18, 18)
                        .addComponent(txtEnquiryContact, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel11)
                        .addGap(18, 18, 18)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 413, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(txtEnquiryMemName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7)
                    .addComponent(txtEnquiryContact, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13)
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel12)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane3.setViewportView(jTable2);

        jButton12.setBackground(new java.awt.Color(255, 204, 0));
        jButton12.setText("Load All Enquiry");
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane3)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jButton12, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton12)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Manage Gym Enquiry", jPanel3);

        jPanel7.setBackground(new java.awt.Color(45, 51, 62));

        jLabel30.setBackground(new java.awt.Color(54, 94, 157));
        jLabel30.setFont(new java.awt.Font("Aparajita", 1, 36)); // NOI18N
        jLabel30.setForeground(new java.awt.Color(255, 255, 255));
        jLabel30.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel30.setText(" Fitness & Health Organization");
        jLabel30.setOpaque(true);

        jTable3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane6.setViewportView(jTable3);

        jLabel32.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel32.setForeground(new java.awt.Color(255, 255, 255));
        jLabel32.setText("Select Option");

        jComboBox5.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Pending Payment", "Subscription End", "Customer Report" }));
        jComboBox5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox5ActionPerformed(evt);
            }
        });

        jButton6.setBackground(new java.awt.Color(255, 204, 0));
        jButton6.setText("Show Log");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jLabel8.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 204, 0));
        jLabel8.setText("Pending Payment Report");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel30, javax.swing.GroupLayout.DEFAULT_SIZE, 1049, Short.MAX_VALUE)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(jLabel32)
                                .addGap(18, 18, 18)
                                .addComponent(jComboBox5, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel30, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel32)
                    .addComponent(jComboBox5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton6))
                .addGap(18, 18, 18)
                .addComponent(jLabel8)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 286, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(53, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("View Report", jPanel7);

        jPanel8.setBackground(new java.awt.Color(45, 51, 62));

        jLabel31.setBackground(new java.awt.Color(54, 94, 157));
        jLabel31.setFont(new java.awt.Font("Aparajita", 1, 36)); // NOI18N
        jLabel31.setForeground(new java.awt.Color(255, 255, 255));
        jLabel31.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel31.setText(" Fitness & Health Organization");
        jLabel31.setOpaque(true);

        jLabel33.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel33.setForeground(new java.awt.Color(255, 255, 255));
        jLabel33.setText("Member ID");

        jButton7.setBackground(new java.awt.Color(255, 204, 0));
        jButton7.setText("Display Log");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jTable4.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane7.setViewportView(jTable4);

        jLabel34.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel34.setForeground(new java.awt.Color(255, 255, 255));
        jLabel34.setText("Select Option");

        jComboBox6.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Member Wise", "Day Wise" }));

        jLabel9.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(255, 204, 0));
        jLabel9.setText("Member Wise Attendance Log");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel31, javax.swing.GroupLayout.DEFAULT_SIZE, 1049, Short.MAX_VALUE)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                        .addComponent(jLabel34)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jComboBox6, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel33)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(35, 35, 35)
                        .addComponent(jButton7, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(310, 310, 310))
                    .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel31, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel33)
                    .addComponent(jButton7)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel34)
                    .addComponent(jComboBox6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel9)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 286, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(53, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("View Attendance Log", jPanel8);

        jPanel16.setBackground(new java.awt.Color(11, 174, 209));

        jPanel17.setBackground(new java.awt.Color(11, 174, 209));
        jPanel17.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jPanel18.setBackground(new java.awt.Color(45, 51, 62));

        jLabel10.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(255, 255, 255));
        jLabel10.setText("Total Gym Fees");

        jLabel15.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(255, 255, 255));
        jLabel15.setText("Fees Paid");

        jLabel35.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel35.setForeground(new java.awt.Color(255, 255, 255));
        jLabel35.setText("Fees Instalment");

        jButton16.setBackground(new java.awt.Color(255, 204, 0));
        jButton16.setText("Search");
        jButton16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton16ActionPerformed(evt);
            }
        });

        jButton17.setBackground(new java.awt.Color(151, 213, 76));
        jButton17.setText("Print Receipt");
        jButton17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton17ActionPerformed(evt);
            }
        });

        jLabel37.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel37.setForeground(new java.awt.Color(255, 255, 255));
        jLabel37.setText("Member ID");

        jButton18.setBackground(new java.awt.Color(151, 213, 76));
        jButton18.setText("Submit");
        jButton18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton18ActionPerformed(evt);
            }
        });

        jLabel38.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel38.setForeground(new java.awt.Color(255, 255, 255));
        jLabel38.setText("Fees Pending");

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel18Layout.createSequentialGroup()
                        .addComponent(jLabel37)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField5))
                    .addComponent(jButton16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel18Layout.createSequentialGroup()
                        .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jTextField1, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel15)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel18Layout.createSequentialGroup()
                        .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel35)
                            .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel38)
                            .addGroup(jPanel18Layout.createSequentialGroup()
                                .addComponent(jButton18, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton17, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel18Layout.setVerticalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel37)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jButton16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel38)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel35)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton17)
                    .addComponent(jButton18))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel36.setBackground(new java.awt.Color(54, 94, 157));
        jLabel36.setFont(new java.awt.Font("Aparajita", 1, 36)); // NOI18N
        jLabel36.setForeground(new java.awt.Color(255, 255, 255));
        jLabel36.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel36.setText(" Fitness & Health Organization");
        jLabel36.setOpaque(true);

        jTable5.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane8.setViewportView(jTable5);

        javax.swing.GroupLayout jPanel19Layout = new javax.swing.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel19Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane8)
                .addContainerGap())
        );
        jPanel19Layout.setVerticalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel19Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane8, javax.swing.GroupLayout.PREFERRED_SIZE, 286, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel36, javax.swing.GroupLayout.DEFAULT_SIZE, 1027, Short.MAX_VALUE)
                    .addGroup(jPanel17Layout.createSequentialGroup()
                        .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel36, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(203, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1069, Short.MAX_VALUE)
            .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel15Layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 638, Short.MAX_VALUE)
            .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel15Layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );

        jTabbedPane1.addTab("Member Payment ", jPanel15);

        jPanel2.setBackground(new java.awt.Color(11, 174, 209));

        jPanel4.setBackground(new java.awt.Color(11, 174, 209));
        jPanel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jPanel9.setBackground(new java.awt.Color(45, 51, 62));

        jLabel1.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Enter Old Password");

        jLabel2.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Enter New Password");

        jLabel3.setFont(new java.awt.Font("Andalus", 0, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Re-Enter Password");

        jButton1.setBackground(new java.awt.Color(255, 204, 0));
        jButton1.setText("Submit");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPasswordField1)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPasswordField2, javax.swing.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPasswordField3)
                    .addComponent(jButton1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPasswordField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPasswordField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel4.setBackground(new java.awt.Color(54, 94, 157));
        jLabel4.setFont(new java.awt.Font("Aparajita", 1, 36)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText(" Fitness & Health Organization");
        jLabel4.setOpaque(true);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addGap(0, 405, Short.MAX_VALUE)
                        .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(368, 368, 368)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(183, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Password Management", jPanel2);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 559, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        change_Password();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
        add_Enquiry();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
        if (jTable2.getSelectedRow() != -1) {
            delete_Enquiry();
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // TODO add your handling code here:
        try {
            txtEnquiryMemName.setText("");
            txtEnquiryContact.setText("");
            jTextArea1.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // TODO add your handling code here:
        add_Member();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        // TODO add your handling code here:
        update_Member();
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        // TODO add your handling code here:
        if (memberSearched) {
            mark_Member_Attendance();
        }

    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        // TODO add your handling code here:
        search_Member();
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        // TODO add your handling code here:
        if (jComboBox5.getSelectedIndex() == 0) {
            jLabel8.setText("Pending Payment Report");
            Load_Fees_Pending_Log();
        } else if (jComboBox5.getSelectedIndex() == 1) {
            jLabel8.setText("Subscription End Report");
            Load_Subscription_End_Log();
        } else if (jComboBox5.getSelectedIndex() == 2) {
            jLabel8.setText("Customer Report");
            Load_Customer_Log();
        }

    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        // TODO add your handling code here:
        Load_Attedance_Log();
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        // TODO add your handling code here:
        if (txtHeight.getText().isEmpty() || txtWeight.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Weight and Height Required");
            return;
        }
        calculate_BMI();


    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
        // TODO add your handling code here:
        Load_Enquiry();
    }//GEN-LAST:event_jButton12ActionPerformed

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
        // TODO add your handling code here:
        clear_All_Fields();
    }//GEN-LAST:event_jButton13ActionPerformed

    private void jComboBox4ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBox4ItemStateChanged
        // TODO add your handling code here:
        if (jComboBox4.getSelectedIndex() != -1) {
            int increment = 0;
            if (jComboBox4.getSelectedIndex() == 0) {
                increment = 1;
            } else if (jComboBox4.getSelectedIndex() == 1) {
                increment = 3;
            } else if (jComboBox4.getSelectedIndex() == 2) {
                increment = 6;
            } else if (jComboBox4.getSelectedIndex() == 3) {
                increment = 12;
            }
            find_nextDate(increment);

        }
    }//GEN-LAST:event_jComboBox4ItemStateChanged

    private void lblSubscriptionEndDisplayMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblSubscriptionEndDisplayMouseClicked
        // TODO add your handling code here:
        jTabbedPane1.setSelectedIndex(2);
        Load_Subscription_End_Log();
    }//GEN-LAST:event_lblSubscriptionEndDisplayMouseClicked

    private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton15ActionPerformed
        // TODO add your handling code here:
        delete_Member();
    }//GEN-LAST:event_jButton15ActionPerformed

    private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton16ActionPerformed
        // TODO add your handling code here:
        memberSearchedPayment = false;
        search_Member_Payment();
    }//GEN-LAST:event_jButton16ActionPerformed

    private void jButton17ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton17ActionPerformed
        // TODO add your handling code here:
        if (memberSearchedPayment) {
            timer = new Timer();
            timerTask1 = new GenerateBillReport();
            timer.schedule(timerTask1, 10);
        }


    }//GEN-LAST:event_jButton17ActionPerformed

    private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton18ActionPerformed
        // TODO add your handling code here:
        insert_Member_fees();
    }//GEN-LAST:event_jButton18ActionPerformed

    private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton14ActionPerformed
        // TODO add your handling code here:
        this.setVisible(false);
        new LoginFrm().setVisible(true);
    }//GEN-LAST:event_jButton14ActionPerformed

    private void jButton19ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton19ActionPerformed
        // TODO add your handling code here:
        remove_Member_Photo();
    }//GEN-LAST:event_jButton19ActionPerformed

    private void jComboBox5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox5ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox5ActionPerformed

    private void txtMemberIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMemberIdActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtMemberIdActionPerformed

    private void txtEnquiryMemNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtEnquiryMemNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtEnquiryMemNameActionPerformed

    private void lblPhotoMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblPhotoMouseReleased
        // TODO add your handling code here:
        Load_Member_Photo();
    }//GEN-LAST:event_lblPhotoMouseReleased


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton17;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton19;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jComboBox3;
    private javax.swing.JComboBox jComboBox4;
    private javax.swing.JComboBox jComboBox5;
    private javax.swing.JComboBox jComboBox6;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JPasswordField jPasswordField2;
    private javax.swing.JPasswordField jPasswordField3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable jTable3;
    private javax.swing.JTable jTable4;
    private javax.swing.JTable jTable5;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JLabel lblPhoto;
    private javax.swing.JLabel lblSubscriptionEndDisplay;
    private javax.swing.JTextArea txtAddress;
    private javax.swing.JTextField txtAge;
    private javax.swing.JTextField txtBmi;
    private javax.swing.JTextField txtEnquiryContact;
    private javax.swing.JTextField txtEnquiryMemName;
    private javax.swing.JTextField txtHeight;
    private javax.swing.JTextField txtMemberContact;
    private javax.swing.JTextField txtMemberId;
    private javax.swing.JTextField txtMemberName;
    private javax.swing.JTextField txtSubscriptionEndDate;
    private javax.swing.JTextField txtSubscriptionStartDate;
    private javax.swing.JLabel txtSubscriptionStatus;
    private javax.swing.JTextField txtTotalFees;
    private javax.swing.JTextField txtWeight;
    // End of variables declaration//GEN-END:variables
}
