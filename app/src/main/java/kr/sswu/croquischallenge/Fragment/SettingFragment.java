package kr.sswu.croquischallenge.Fragment;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;

import kr.sswu.croquischallenge.FeedListActivity;
import kr.sswu.croquischallenge.R;
import kr.sswu.croquischallenge.likeActivity;
import kr.sswu.croquischallenge.login.activity.LoginActivity;

import static android.content.Context.MODE_PRIVATE;

public class SettingFragment extends Fragment {

    public static final String ex = "sw";

    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reference;

    private ProgressDialog progressDialog;

    private String uEmail;
    private TextView uName;
    private Button editName;
    private TextView logout;
    private RelativeLayout feedList, likeList;
    private Switch sw;
    SharedPreferences sharedPreferences;
    private int exist;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        reference = firebaseDatabase.getReference("Users");

        progressDialog = new ProgressDialog(getActivity());

        uName = (TextView) view.findViewById(R.id.txt_uName);
        editName = (Button) view.findViewById(R.id.btn_editName);
        feedList = (RelativeLayout) view.findViewById(R.id.layout_feedList);
        likeList = (RelativeLayout) view.findViewById(R.id.layout_likeList);
        sw = (Switch) view.findViewById(R.id.sw);
        logout = (TextView) view.findViewById(R.id.txt_logout);

        //사용자 이름 default = 이메일 주소
        //사용자 이름 변경한 경우 setting 기본 화면 상단에 변경된 이름 출력
        uEmail = user.getEmail();

        Query query = reference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.child("name").getValue().toString();

                    if (name.equals(""))
                        uName.setText(uEmail);
                    else
                        uName.setText(name);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // 사용자 이름 수정
        editName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditNameDialog();
            }
        });

        //피드 리스트
        feedList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), FeedListActivity.class);
                intent.putExtra("uid", user.getUid());
                startActivity(intent);
            }
        });

        //좋아요 리스트
        likeList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), likeActivity.class);
                intent.putExtra("uid", user.getUid());
                startActivity(intent);
            }
        });

        //푸시 알림 onoff 스위치 추가
        //토픽으로 제어
        sharedPreferences = getActivity().getSharedPreferences(" ", MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        sw.setChecked(sharedPreferences.getBoolean(ex, true));
        FirebaseMessaging.getInstance().subscribeToTopic("1");
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editor.putBoolean(ex, true); // value to store
                    FirebaseMessaging.getInstance().subscribeToTopic("1");
                } else {
                    editor.putBoolean(ex, false); // value to store
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("1");
                }
                editor.commit();
            }
        });

        //fcm cloudmessage token
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("FCM Log", "getInstanceId failed", task.getException());
                            return;
                        }
                        String token = task.getResult().getToken();

                        Log.d("FCM Log", "FCM 토큰: " + token);
                        //Toast.makeText(MainActivity.this, token, Toast.LENGTH_SHORT).show();

                    }
                });

        //로그아웃
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseAuth.signOut();
                checkUser();
                getActivity().onBackPressed();
            }
        });


        return view;
    }

    private void showEditNameDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Edit Name");

        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(15, 15, 15, 15);

        EditText edit_name = new EditText(getActivity());
        TextView message = new TextView(getActivity());
        edit_name.setHint("Username");
        linearLayout.addView(edit_name);
        linearLayout.addView(message);

        builder.setView(linearLayout);
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                exist = 0;
                String uName = edit_name.getText().toString().trim();

                //사용자 이름 중복 확인
                if (!TextUtils.isEmpty(uName)) {
                    Query query = reference.orderByChild("name").equalTo(uName);
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                exist++;
                                Toast.makeText(getContext(), "The user name is already in use", Toast.LENGTH_LONG).show();
                                return;
                            }

                            if (exist == 0) {
                                Toast.makeText(getContext(), "Confirm Successfully", Toast.LENGTH_SHORT).show();
                                progressDialog.show();
                                HashMap<String, Object> edit_result = new HashMap<>();
                                edit_result.put("name", uName);

                                reference.child(user.getUid()).updateChildren(edit_result)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                progressDialog.dismiss();
                                                Toast.makeText(getContext(), "Updated..", Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressDialog.dismiss();
                                        Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Enter User Name", Toast.LENGTH_SHORT).show();
                }

            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.create().show();
    }

    private void checkUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null)
            startActivity(new Intent(getContext(), LoginActivity.class));
    }
}

//푸시알람 코드 출처
//https://m.blog.naver.com/PostView.nhn?blogId=tkddlf4209&logNo=221483174037&proxyReferer=https:%2F%2Fwww.google.com%2F
//https://www.youtube.com/watch?v=mIE73hnu4I4