package com.alex.bingo.Activity;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alex.bingo.Bean.NumberButton;
import com.alex.bingo.R;
import com.alex.bingo.Bean.Room;
import com.firebase.ui.common.ChangeEventType;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BingoActivity extends AppCompatActivity {

    private static final int NUMBER_COUNT = 25;
    private static final String TAG = BingoActivity.class.getSimpleName();
    private String roomId;
    private RecyclerView recyclerView;
    private TextView info;
    private boolean creator;
    private List<Integer> randomNumbers;
    private FirebaseRecyclerAdapter<Boolean, NumberHolder> adapter;
    private List<NumberButton> buttons;
    private Map<Integer,NumberButton> numberMap = new HashMap<>();
    boolean myTurn = false;


    final ValueEventListener statusListener = new ValueEventListener() {

        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            long status = (long) dataSnapshot.getValue();
            switch ((int) status) {
                case Room.STATUS_CREATED:
                    info.setText(getString(R.string.waiting_player));
                    break;
                case Room.STATUS_JOINED:
                    info.setText(getString(R.string.opponent_join));
                    setMyTurn(isCreator() ? true : false);
                    FirebaseDatabase.getInstance().getReference("rooms")
                            .child(roomId)
                            .child("status")
                            .setValue(Room.STATUS_CREATOR_TURN);
                    break;
                case Room.STATUS_CREATOR_TURN:
                    setMyTurn(isCreator() ? true : false);
                    break;
                case Room.STATUS_JOINERS_TURN:
                    setMyTurn(!isCreator() ? true : false);
                    break;
                case Room.STATUS_CREATOR_BINGO:
                    if (!isCreator()) {
                        new AlertDialog.Builder(BingoActivity.this)
                                .setTitle(R.string.loser)
                                .setMessage(R.string.you_lose)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //TODO: clean room
                                        endGame();
                                    }
                                }).show();
                    }
                    break;
                case Room.STATUS_JOINERS_BINGO:
                    if (isCreator()) {
                        new AlertDialog.Builder(BingoActivity.this)
                                .setTitle(R.string.loser)
                                .setMessage(R.string.you_lose)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //TODO: clean room
                                        endGame();
                                    }
                                }).show();
                    }
                    break;
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    private void endGame() {
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId)
                .child("status")
                .removeEventListener(statusListener);
        if (isCreator()){
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .removeValue();
        }
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bingo);
        findViews();
        roomId = getIntent().getStringExtra("ROOM_ID");
        creator = getIntent().getBooleanExtra("IS_CREATOR",false);
        generateRandomNumbers();

        if (isCreator()){
            //fill firebase room numbers
            for (int i = 0; i < NUMBER_COUNT; i++) {
                FirebaseDatabase.getInstance().getReference("rooms")
                        .child(roomId)
                        .child("numbers")
                        .child(i+1+"")
                        .setValue(false);
            }
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .child("status")
                    .setValue(Room.STATUS_CREATED);
        }
        else { //for joiner
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .child("status")
                    .setValue(Room.STATUS_JOINED);
        }
        //Recyclerview
        Query query = FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId)
                .child("numbers")
                .orderByKey();
        FirebaseRecyclerOptions<Boolean> options = new FirebaseRecyclerOptions.Builder<Boolean>()
                .setQuery(query,Boolean.class).build();
        adapter = new FirebaseRecyclerAdapter<Boolean, NumberHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull NumberHolder holder, final int position, @NonNull Boolean model) {
                holder.button.setText(buttons.get(position).getNumber()+"");
                holder.button.setEnabled(!buttons.get(position).isPicked());
                holder.button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isMyTurn()){
                            int number = buttons.get(position).getNumber();
                            FirebaseDatabase.getInstance().getReference("rooms")
                                    .child(roomId)
                                    .child("numbers")
                                    .child(number+"")
                                    .setValue(true);
                        }
                    }
                });
            }

            @Override
            public void onChildChanged(@NonNull ChangeEventType type, @NonNull DataSnapshot snapshot, int newIndex, int oldIndex) {
                super.onChildChanged(type, snapshot, newIndex, oldIndex);
                Log.d(TAG, "onChildChanged: " + type.name() + "/" + snapshot.getKey());
                if (type == ChangeEventType.CHANGED){
                    NumberButton numberButton = numberMap.get(Integer.parseInt(snapshot.getKey()));
                    int pos = numberButton.getPosition();
                    NumberHolder holder = (NumberHolder) recyclerView.findViewHolderForAdapterPosition(pos);
                    holder.button.setEnabled(false);
                    numberButton.setPicked(true);
                    if (isMyTurn()){
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomId)
                                .child("status")
                                .setValue(isCreator() ? Room.STATUS_JOINERS_TURN : Room.STATUS_CREATOR_TURN);
                        //Bingo
                        int [] nums = new int[NUMBER_COUNT];
                        for (int i = 0; i < NUMBER_COUNT; i++) {
                            nums[i] = buttons.get(i).isPicked() ? 1 : 0;
                        }
                        int bingo = 0;
                        int sum = 0;
                        for (int i = 0; i < 5; i++) {
                            sum = 0;
                            for (int j = 0; j < 5; j++) {
                                sum = sum + nums[i * 5 + j];
                            }
                            bingo += (sum == 5) ? 1 : 0;
                            sum = 0;
                            for (int j = 0; j < 5; j++) {
                                sum = sum + nums[j * 5 + i];
                            }
                            bingo += (sum == 5) ? 1 : 0;
                        }
                        Log.d(TAG, "onChildChanged:bingo " + bingo);
                        if (bingo > 0){
                            FirebaseDatabase.getInstance().getReference("rooms")
                                    .child(roomId)
                                    .child("status")
                                    .setValue(isCreator() ? Room.STATUS_CREATOR_TURN: Room.STATUS_JOINERS_TURN);
                            new AlertDialog.Builder(BingoActivity.this)
                                    .setTitle(R.string.bingo)
                                    .setMessage(R.string.bingo_info)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            //TODO: clean room
                                            endGame();
                                        }
                                    }).show();
                        }
                    }
                }
            }

            @NonNull
            @Override
            public NumberHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(BingoActivity.this).inflate(R.layout.single_number,viewGroup,false);
                return new NumberHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId)
                .child("status")
                .addValueEventListener(statusListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId)
                .child("status")
                .removeEventListener(statusListener);
        if (isCreator()){
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .removeValue();
        }
        finish();
    }

    class NumberHolder extends RecyclerView.ViewHolder{
        NumberButton button;
        public NumberHolder(@NonNull View itemView) {
            super(itemView);
            button = itemView.findViewById(R.id.number);
        }
    }

    private void generateRandomNumbers() {
        randomNumbers = new ArrayList<>();
        for (int i = 0; i < NUMBER_COUNT; i++) {
            randomNumbers.add(i+1);
        }
        Collections.shuffle(randomNumbers);
        buttons = new ArrayList<>();
        for (int i = 0; i < NUMBER_COUNT; i++) {
            NumberButton button = new NumberButton(this);
            button.setText(randomNumbers.get(i)+"");
            button.setNumber(randomNumbers.get(i));
            button.setPosition(i);
            buttons.add(button);
            numberMap.put(button.getNumber(),button);
        }
    }

    private void findViews() {
        recyclerView = findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this,5));
        info = findViewById(R.id.info);
    }

    public boolean isCreator() {
        return creator;
    }

    public void setCreator(boolean creator) {
        this.creator = creator;
    }

    public boolean isMyTurn() {
        return myTurn;
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
        info.setText(myTurn ? getString(R.string.select_number) : getString(R.string.waitting_opponent));
    }

}
