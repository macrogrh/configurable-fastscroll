# Configurable-FastScroll

Configurable fastscroll of `RecyclerView`.
This is modified FastScroller widget from AOSP. 


## Preview


<p><img src="readme/configurable_height_fastscroll (default).gif" width="30%" />
<img src="readme/configurable_height_fastscroll (image).gif" width="30%" /></p>

## Usage
Include the library

 ```
 dependencies {
     implementation 'com.github.macrogrh:configurable-fastscroll:1.0.1'
 }
 ```
Add configurable-fastscroll item decoration to recyclerview
 ```
recyclerView.addItemDecoration((new ConfigurableFastScroll.Builder(recyclerView)
        .setThumbDrawable(R.drawable.sample_thumb)
        .setHeight(R.dimen.fastscroll_thumb_height)
        .setTopBottomMargin(R.dimen.fastscroll_thumb_top_bottom_margin)
        .setRightMargin(R.dimen.fastscroll_thumb_right_margin)
        .setThumbTint(R.color.colorPrimary)
        .build()));
 ```
### Options
- `setThumbDrawable()` set thumb drawable. StateListDrawable is also allowed. 
- `setHeight()` set thumb height. Default value is thumbDrawable's height.
- `setTopBottomMargin()` set margin of top & bottom margin. Default value is 0dp.
- `setRightMargin()` set right margin. Default value is 0dp.
- `setThumbTint` set tint color of drawable. 

## Not supported
- different row height : may cause strange scroll behavior
- LAYOUT_DIRECTION_RTL
- vertical scroll
- thumb track

## License
 ```
    Copyright macrogrh

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 ```