/**
 * Base activity which provides:
 * <ul>
 *   <li>gestures detection by {@link GlassGestureDetector}</li>
 *   <li>reaction for {@link Gesture#SWIPE_DOWN} gesture as finishing current activity</li>
 *   <li>hiding system UI</li>
 * </ul>
 *//*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.synople.glassecho.glass;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import dev.synople.glassecho.glass.utils.GlassGestureDetector;
import dev.synople.glassecho.glass.utils.GlassGestureDetector.Gesture;
import dev.synople.glassecho.glass.utils.GlassGestureDetector.OnGestureListener;

/**
 * Base activity which provides:
 * <ul>
 *   <li>gestures detection by {@link GlassGestureDetector}</li>
 *   <li>reaction for {@link Gesture#SWIPE_DOWN} gesture as finishing current activity</li>
 *   <li>hiding system UI</li>
 * </ul>
 */
public abstract class BaseActivity extends AppCompatActivity implements OnGestureListener {

  private View decorView;
  private GlassGestureDetector glassGestureDetector;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
    }
    decorView = getWindow().getDecorView();
    decorView
        .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
          @Override
          public void onSystemUiVisibilityChange(int visibility) {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
              hideSystemUI();
            }
          }
        });
    glassGestureDetector = new GlassGestureDetector(this, this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    hideSystemUI();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (glassGestureDetector.onTouchEvent(ev)) {
      return true;
    }
    return super.dispatchTouchEvent(ev);
  }

  @Override
  public boolean onGesture(Gesture gesture) {
    switch (gesture) {
      case SWIPE_DOWN:
        finish();
        return true;
      default:
        return false;
    }
  }

  private void hideSystemUI() {
    decorView.setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_IMMERSIVE
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN);
  }
}
