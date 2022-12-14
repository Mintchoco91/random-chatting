package com.kj.random_chatting.util;

import android.content.Context;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.kj.random_chatting.userChattingRoomList.UserChattingRoomListDTO;
import com.kj.random_chatting.userChattingRoomList.UserChattingRoomListFragment;
import com.kj.random_chatting.databinding.FragmentUserChattingRoomListBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 공통으로 사용 할 Util 함수 정의
 */

public class UtilClass {

    private UtilClass utilClass;
    /**
     * textView 스크롤바 넘어갈 시 밑으로 자동 스크롤처리
     * [textView객체].setMovementMethod(new ScrollingMovementMethod()) 선언 후 사용 할 것
     */

    public void scrollBottom(TextView textView) {
        int lineTop = textView.getLayout().getLineTop(textView.getLineCount());
        int scrollY = lineTop - textView.getHeight();
        if (scrollY > 0) {
            textView.scrollTo(0, scrollY);
        } else {
            textView.scrollTo(0, 0);
        }
    }

    /**
     * 해당 자리수 까지 랜덤 처리
     * @param digit : 자릿수
     */
    public Integer createRandomNumber(Integer digit){
        Random random = new Random();

        String strMaxNumber = String.format("%0"+digit+"d", 0);
        Integer maxNumber = Integer.parseInt(strMaxNumber.replace("0","9"));
        Integer randNumber = random.nextInt(maxNumber);

        return randNumber;
    }
}
