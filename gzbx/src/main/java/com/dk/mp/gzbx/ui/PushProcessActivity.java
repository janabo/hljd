package com.dk.mp.gzbx.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.dk.mp.core.entity.GsonData;
import com.dk.mp.core.entity.JsonData;
import com.dk.mp.core.http.HttpUtil;
import com.dk.mp.core.http.request.HttpListener;
import com.dk.mp.core.ui.MyActivity;
import com.dk.mp.core.util.BroadcastUtil;
import com.dk.mp.core.util.DeviceUtil;
import com.dk.mp.gzbx.Adapter.MyProcessAdapter;
import com.dk.mp.gzbx.R;
import com.dk.mp.gzbx.entity.Malfunction;
import com.dk.mp.gzbx.entity.ProcessInfo;
import com.dk.mp.gzbx.util.StringUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 故障报修推送流程页面
 * @author admin
 *
 */
public class PushProcessActivity extends MyActivity implements OnClickListener{
	private MyProcessAdapter mAdapter;
	private Malfunction m;
	private TextView textView1,textView2;
	private TextView status;
	private TextView address,device,des;
	private ListView listview;
	private LinearLayout bottom_view;
	private LinearLayout bottom_left;
	private LinearLayout bottom_right;
	private LinearLayout bottom_view2;//指定维修人
	private Context context;
	private Map<String,Object> map;
	private TextView tro_end_repair;
	public static PushProcessActivity instance = null;
	private String fgxxInfo ="";
	private String tywxInfo = "";
	private String alertLeftMsg="";//左侧弹出框标题
	private String alertLeftType="";//左侧弹出框弹出类型
	private String alertRightMsg="";//右侧弹出框标题
	private String alertRightType="";//右侧弹出框弹出类型

	@Override
	protected int getLayoutID() {
		return R.layout.layout_my_process_troublshooting;
	}

	@Override
	protected void initialize() {
		super.initialize();

		setTitle("故障报修");
		instance = this;
		context = PushProcessActivity.this;
		findView();
		getData();
	}
	
	private void findView(){
		Intent intent = getIntent();
		m = (Malfunction) intent.getSerializableExtra("malfunction");
		textView1 = (TextView) findViewById(R.id.textView1);
		textView2 = (TextView) findViewById(R.id.textView2);
		tro_end_repair = (TextView) findViewById(R.id.tro_end_repair);
		status = (TextView) findViewById(R.id.status);
		address = (TextView) findViewById(R.id.address);
		device = (TextView) findViewById(R.id.device);
		des = (TextView) findViewById(R.id.des);
		listview = (ListView) findViewById(R.id.listview);
		bottom_view = (LinearLayout) findViewById(R.id.bottom_view);
		bottom_left = (LinearLayout) findViewById(R.id.bottom_left);
		bottom_right = (LinearLayout) findViewById(R.id.bottom_right);
		bottom_view2 = (LinearLayout) findViewById(R.id.bottom_view2);
		listview.setDividerHeight(0);
		bottom_view2.setOnClickListener(this);
		bottom_left.setOnClickListener(this);
		bottom_right.setOnClickListener(this);

		String name = m.getName();
		textView1.setText(StringUtil.dealString(name));
		textView2.setText(m.getTitle());
		status.setText(m.getStatusname());
		address.setText(m.getAddress());
		device.setText(m.getDevice());
		des.setText(m.getDes());
	}
	
	/**
	 * 获取维修详细信息
	 */
	public void getData(){
		if(DeviceUtil.checkNet()){
			Map<String,Object> _map = new HashMap<String, Object>();
			_map.put("id",  m.getId());
			HttpUtil.getInstance().postJsonObjectRequest("apps/gzbx/splc", _map, new HttpListener<JSONObject>() {
				@Override
				public void onSuccess(JSONObject result) {
					try {
						if (result.getInt("code") == 200){
							List<ProcessInfo> list = new Gson().fromJson(result.toString(), new TypeToken<GsonData<ProcessInfo>>() {}.getType());
							map.put("pinfo", list);
							map.put("operation", result.getJSONObject("data").get("operation"));
							map.put("operateState", result.getJSONObject("data").get("operateState"));

							if(map.isEmpty()){
								mHandler.sendEmptyMessage(2);
							}else{
								mHandler.sendEmptyMessage(1);
							}
                        }
					} catch (JSONException e) {
						e.printStackTrace();
						showMessage(getString(R.string.data_error));
					}
				}

				@Override
				public void onError(VolleyError error) {
					showMessage(getString(R.string.data_error));
				}
			});
		}
	}
	
	@SuppressLint("HandlerLeak")
	Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 1:
				if(mAdapter == null){
					@SuppressWarnings("unchecked")
					List<ProcessInfo> list = (List<ProcessInfo>) map.get("pinfo");
						String operation = (String) map.get("operation");
						String operateState = (String)map.get("operateState");
						String lc = list.get(list.size()-1).getLc();
						if("1".equals(lc)){
							ProcessInfo pinfo = new ProcessInfo();
							pinfo.setId("0");
							pinfo.setName("现教中心");
							pinfo.setStatus("-1");
							pinfo.setStatusname("待审批");
							pinfo.setTime(list.get(list.size()-1).getTime());
							list.add(pinfo);
						}else if("2".equals(lc)){
							ProcessInfo pinfo = new ProcessInfo();
							pinfo.setId("0");
							pinfo.setName("现教中心");
							pinfo.setStatus("-1");
							pinfo.setStatusname("待维修人员维修");
							pinfo.setTime(list.get(list.size()-1).getTime());
							list.add(pinfo);
						}else if("3".equals(lc)){
							ProcessInfo pinfo = new ProcessInfo();
							pinfo.setId("0");
							pinfo.setName("现教中心");
							pinfo.setStatus("-1");
							pinfo.setStatusname("待提报人员反馈");
							pinfo.setTime(list.get(list.size()-1).getTime());
							list.add(pinfo);
						}
						mAdapter = new MyProcessAdapter(context, list);
						listview.setAdapter(mAdapter);
						if("1".equals(operation) && "1".equals(operateState)){
							bottom_view2.setVisibility(View.VISIBLE);
						}else if("2".equals(operation) && "1".equals(operateState)){
							bottom_view.setVisibility(View.VISIBLE);
							tro_end_repair.setText(R.string.tro_aree);
							list.get(list.size()-1).setStatus("-1");
							mAdapter = new MyProcessAdapter(context, list);
							listview.setAdapter(mAdapter);
							alertLeftMsg = "请填写维修情况";
							alertLeftType = "wxxx";
							alertRightMsg = "请填写指导建议";
							alertRightType = "zdxx";
						}else if("0".equals(operation) && "1".equals(operateState)){
							tro_end_repair.setText(R.string.tro_end_repair);
							bottom_view.setVisibility(View.VISIBLE);
							alertLeftMsg = "";
							alertLeftType = "ywx";
							alertRightMsg = "请填写维修反馈情况";
							alertRightType = "fkxx";
						}else{
							bottom_view.setVisibility(View.GONE);
						}
					}
				break;
			case 2:
				showMessage(getString(R.string.data_error));
				break;
			case 3:
				finish();
				BroadcastUtil.sendBroadcast(context, "com.test.action.refresh");
				BroadcastUtil.sendBroadcast(context, "com.test.action.refresh.tab");
				showMessage("提交成功");
				break;
			case 4:
				showMessage("提交失败，请重试！");
				break;
			default:
				break;
			}
		};
	};
	
	public void alertDialog(final String title,final String type){
		final Dialog dlg = new Dialog(context,R.style.repair_MyDialog);
		Window window = dlg.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		window.setContentView(R.layout.repair_dialog);
		dlg.show();
		TextView titleView = (TextView) window.findViewById(R.id.title);
		titleView.setText(title);
		final EditText contentView = (EditText) window.findViewById(R.id.content);
		if("zdxx".equals(type)){
			contentView.setText(fgxxInfo);
		}else{
			contentView.setText(tywxInfo);
		}
		contentView.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				if(contentView.getText().toString().trim().length() >= 140){
					showMessage("请填写少于140个字");
				}
			}
		});
	
		final Button ok = (Button) window.findViewById(R.id.ok);
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ok.setEnabled(false);
				final String contextText = contentView.getText().toString().trim();
				if(contextText.length()<=0 && !"ywx".equals(type)){
					showMessage(title);
					ok.setEnabled(true);
					return ;
				}else if(contextText.length()>140){
					showMessage("请填写少于140个字");
					ok.setEnabled(true);
					return;
				}else{
					if(DeviceUtil.checkNet()){
						Map<String,Object> map = new HashMap<String,Object>();
						map.put("type", type);
						map.put("id", m.getId());
						String message = contextText;
						if("zdxx".equals(type)){
							message = "指导信息:"+message;
						}else if("wxxx".equals(type)){
							message = "维修情况:"+message;
						}else if("fkxx".equals(type)){
							message = "维修反馈:"+message;
						}
						map.put("message", message);
						
						
						if("zdxx".equals(type) || "wxxx".equals(type)){
							HttpUtil.getInstance().postJsonObjectRequest("apps/gzbx/wxrytjxx", map, new HttpListener<JSONObject>() {
								@Override
								public void onSuccess(JSONObject result) {
									JsonData jd = getGson().fromJson(result.toString(),JsonData.class);
									if (jd.getCode() == 200 && (Boolean) jd.getData()) {
                                        mHandler.sendEmptyMessage(3);
                                        ok.setEnabled(true);
                                    }else{
                                        dlg.hide();
                                        mHandler.sendEmptyMessage(4);
                                        ok.setEnabled(true);
                                    }
								}
								@Override
								public void onError(VolleyError error) {
									showMessage(getString(R.string.data_error));
									ok.setEnabled(true);
								}
							});
						}else{
							HttpUtil.getInstance().postJsonObjectRequest("apps/gzbx/tbryfkxx", map, new HttpListener<JSONObject>() {
								@Override
								public void onSuccess(JSONObject result) {
									JsonData jd = getGson().fromJson(result.toString(),JsonData.class);
									if (jd.getCode() == 200 && (Boolean) jd.getData()) {
										mHandler.sendEmptyMessage(3);
										ok.setEnabled(true);
									}else{
										mHandler.sendEmptyMessage(4);
										ok.setEnabled(true);
									}
								}

								@Override
								public void onError(VolleyError error) {
									showMessage(getString(R.string.data_error));
									ok.setEnabled(true);
								}
							});
						}
					}else{
						ok.setEnabled(true);
						dlg.hide();
					}
				}
			}
		});
		Button cancel = (Button) window.findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ok.setEnabled(true);
				dlg.cancel();
			}
		});
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.bottom_view2){
			Intent intent = new Intent(PushProcessActivity.this,ContactPersonActivity.class);
			intent.putExtra("id", m.getId());
			startActivityForResult(intent, 1);
		}else if(v.getId() == R.id.bottom_right){
			alertDialog(alertRightMsg,alertRightType);
		}else if(v.getId() == R.id.bottom_left){
			if(!alertLeftType.equals("ywx")){
				alertDialog(alertLeftMsg,alertLeftType);
			}else{
				if(DeviceUtil.checkNet()){
					Map<String,Object> map = new HashMap<String,Object>();
					map.put("type", "ywx");
					String message = "";
					map.put("id", m.getId());
					map.put("message", message);
					HttpUtil.getInstance().postJsonObjectRequest("apps/gzbx/tbryfkxx", map, new HttpListener<JSONObject>() {
						@Override
						public void onSuccess(JSONObject result) {
							JsonData jd = getGson().fromJson(result.toString(),JsonData.class);
							if (jd.getCode() == 200 && (Boolean) jd.getData()) {
								mHandler.sendEmptyMessage(3);
							}else{
								mHandler.sendEmptyMessage(4);
							}
						}

						@Override
						public void onError(VolleyError error) {
							showMessage(getString(R.string.data_error));
						}
					});
				}
			}
		}
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}
	
}