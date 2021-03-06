package com.example.fzuscore;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class ScoreRankAdapter extends RecyclerView.Adapter<ScoreRankAdapter.ViewHolder> {

    private List<ScoreRankStudent> mStudentList;

    public ScoreRankAdapter(List<ScoreRankStudent> studentList){
        mStudentList = studentList;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.score_item,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ScoreRankStudent student = mStudentList.get(position);
        holder.score.setText(String.valueOf(student.getScore()));
        holder.name.setText(student.getName());
        holder.number.setText(String.valueOf(student.getNumber()));
        holder.rank.setText(String.valueOf(student.getRank()));
    }

    @Override
    public int getItemCount() {
        return mStudentList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView name;
        TextView score;
        TextView number;
        TextView rank;

        public ViewHolder(View view){
            super(view);
            rank = view.findViewById(R.id.score_rank);
            name = view.findViewById(R.id.score_rank_name);
            score = view.findViewById(R.id.score_rank_score);
            number = view.findViewById(R.id.score_rank_number);
        }
    }

}
