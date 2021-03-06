package com.dk.mp.gzbx.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.android.volley.VolleyError;
import com.dk.mp.core.entity.GsonData;
import com.dk.mp.core.http.HttpUtil;
import com.dk.mp.core.http.request.HttpListener;
import com.dk.mp.core.ui.MyActivity;
import com.dk.mp.core.util.BroadcastUtil;
import com.dk.mp.core.util.DeviceUtil;
import com.dk.mp.core.view.listview.XListView;
import com.dk.mp.core.view.listview.XListView.IXListViewListener;
import com.dk.mp.core.widget.ErrorLayout;
import com.dk.mp.gzbx.Adapter.MyTroublshootingAdapter;
import com.dk.mp.gzbx.R;
import com.dk.mp.gzbx.entity.Malfunction;
import com.dk.mp.gzbx.entity.ProcessInfo;
import com.dk.mp.gzbx.http.HttpListArray;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 我的发起的报修列表
 * @author dake
 *
 */
public class MyTroublshootingListActivity extends MyActivity implements IXListViewListener, OnItemClickListener{
	public static final String ACTION_REFRESH = "com.test.action.refresh";
	private Context context = MyTroublshootingListActivity.this;
	private XListView listView;
	private MyTroublshootingAdapter adapter;
	private List<Malfunction> list;
	private String type;
	private int approvalorinitiate;  //1,待我审批 0.我发起的报修
	private Map<String,Object> map;
	private int nextPage =1;
	public static MyTroublshootingListActivity instance;

	private ErrorLayout errorLayout;

	@Override
	protected int getLayoutID() {
		return R.layout.layout_mytab_listview;
	}

	@Override
	protected void initialize() {
		super.initialize();

		instance = this;
		Intent intent = getIntent();
		type = intent.getStringExtra("type");
		approvalorinitiate = intent.getIntExtra("approvalorinitiate", 0);
		findView();
		if(DeviceUtil.checkNet()){
			getData();
		}else{
			errorLayout.setErrorType(ErrorLayout.NETWORK_ERROR);
		}
		BroadcastUtil.registerReceiver(context, receiver, ACTION_REFRESH);
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (ACTION_REFRESH.equals(intent.getAction())) {
				if(DeviceUtil.checkNet()){
					nextPage =1;
					getData();
				}else{
					errorLayout.setErrorType(ErrorLayout.NETWORK_ERROR);
				}
			}
		}
	};
	
	private void findView(){
		listView = (XListView) findViewById(R.id.listView);
		errorLayout = (ErrorLayout) findViewById(R.id.error_layout);
		listView.setPullLoadEnable(true);
		listView.setOnItemClickListener(this);
		listView.setXListViewListener(this);
	}
	
	public void getData(){
		BroadcastUtil.sendBroadcast(context, "com.test.action.refresh.tab");
		if(approvalorinitiate == 0){
			errorLayout.setErrorType(ErrorLayout.LOADDATA);
			Map<String,Object> _map = new HashMap<String, Object>();
			_map.put("type", type);
			_map.put("pageNo", nextPage+"");
			HttpUtil.getInstance().postJsonObjectRequest("apps/gzbx/wdbx", _map, new HttpListener<JSONObject>() {
				@Override
				public void onSuccess(JSONObject result) {
					try {
						if (result.getInt("code") == 200){
							map = HttpListArray.getMyMalfunction(result);

							if(!map.isEmpty() && map.size()>0) {
								list = (List<Malfunction>) map.get("list");
								if (list.size() <= 0 && nextPage == 1) {
									errorLayout.setErrorType(ErrorLayout.NODATA);
									listView.setVisibility(View.GONE);
								} else {
									mHandler.sendEmptyMessage(1);
									listView.setVisibility(View.VISIBLE);
									errorLayout.setErrorType(ErrorLayout.HIDE_LAYOUT);
								}
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
						listView.setVisibility(View.GONE);
						errorLayout.setErrorType(ErrorLayout.DATAFAIL);
					}
				}

				@Override
				public void onError(VolleyError error) {
					listView.setVisibility(View.GONE);
					errorLayout.setErrorType(ErrorLayout.DATAFAIL);
				}
			});
		}else{
			errorLayout.setErrorType(ErrorLayout.HIDE_LAYOUT);
			Map<String,Object> _map = new HashMap<String, Object>();
			_map.put("type", type);
			_map.put("pageNo", nextPage+"");
			HttpUtil.getInstance().postJsonObjectRequest("apps/gzbx/wdsp", _map, new HttpListener<JSONObject>() {
				@Override
				public void onSuccess(JSONObject result) {
					try {
						if (result.getInt("code") == 200){
							map = HttpListArray.getAwaitingApproval(result);
							if(!map.isEmpty() && map.size()>0){
								list = (List<Malfunction>) map.get("list");
								if(list.size()<=0 && nextPage==1){
									errorLayout.setErrorType(ErrorLayout.NODATA);
									listView.setVisibility(View.GONE);
								}else{
									mHandler.sendEmptyMessage(1);
									listView.setVisibility(View.VISIBLE);
									errorLayout.setErrorType(ErrorLayout.HIDE_LAYOUT);
								}
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
						listView.setVisibility(View.GONE);
						errorLayout.setErrorType(ErrorLayout.DATAFAIL);
					}
				}

				@Override
				public void onError(VolleyError error) {

				}
			});
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Malfunction malfunction = list.get(position-1);
		Intent intent;
		if(approvalorinitiate == 0){
			intent = new Intent(MyTroublshootingListActivity.this,MyTroublshootingProcessActivity.class);
		}else{
			intent = new Intent(MyTroublshootingListActivity.this,AwaitApprovalProcessActivity.class);
		}
	
		Bundle bundle = new Bundle();
		bundle.putSerializable("malfunction", malfunction);
		bundle.putString("type", type);
		intent.putExtras(bundle);
		startActivity(intent);
	}
	
	@SuppressLint("HandlerLeak")
	Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 1:
				if(adapter==null){
					adapter = new MyTroublshootingAdapter(context, list);
					listView.setAdapter(adapter);
				}else{
					adapter.setList(list);
					adapter.notifyDataSetChanged();
				}
				int totalPages = (Integer) map.get("totalPages");
				int currentPage = (Integer) map.get("currentPage");
				nextPage = (Integer) map.get("nextPage");
				if(totalPages <= currentPage){
					listView.hideFooter();
				}else{
					listView.showFooter();
				}
				listView.stopRefresh();
				listView.stopLoadMore();
				break;
			case 2:
				showMessage("已经到底了");
				listView.hideFooter();
				break;
			default:
				break;
			}
		};
	};

	@Override
	public void onRefresh() {
		if(DeviceUtil.checkNet()){
			nextPage = 1;
			getData();
		}else{
			errorLayout.setErrorType(ErrorLayout.NETWORK_ERROR);
			listView.setVisibility(View.GONE);
		}
	}

	@Override
	public void onLoadMore() {
		if(DeviceUtil.checkNet()){
			getList();
		}
	}

	@Override
	public void stopLoad() {
		listView.stopRefresh();
		listView.stopLoadMore();
	}

	@Override
	public void getList() {
		if(approvalorinitiate == 0){
			Map<String,Object> _map = new HashMap<String, Object>();
			_map.put("type", type);
			_map.put("pageNo", nextPage+"");
			HttpUtil.getInstance().postJsonObjectRequest("apps/gzbx/wdbx", _map, new HttpListener<JSONObject>() {
				@Override
				public void onSuccess(JSONObject result) {
					try {
						if (result.getInt("code") == 200){
							map = HttpListArray.getMyMalfunction(result);
							if(!map.isEmpty() && map.size()>0){
								List<Malfunction> mafs = (List<Malfunction>) map.get("list");
								list.addAll(mafs);
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
		}else{
			Map<String,Object> _map = new HashMap<String, Object>();
			_map.put("type", type);
			_map.put("pageNo", nextPage+"");
			HttpUtil.getInstance().postJsonObjectRequest("apps/gzbx/wdsp", _map, new HttpListener<JSONObject>() {
				@Override
				public void onSuccess(JSONObject result) {
					try {
						if (result.getInt("code") == 200){

							map = HttpListArray.getAwaitingApproval(result);
							if(!map.isEmpty() && map.size()>0){
								List<Malfunction> mafs = (List<Malfunction>) map.get("list");
								list.addAll(mafs);
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
	
}
