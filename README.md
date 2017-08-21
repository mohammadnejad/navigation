# Navigation
Car navigation with google map.
Routing from current location to specified destination.

### Getting Started
Download Navigationlibrary from github and add this library to your project.

### How to use
Create new activity and add below segment code to onCreate() method

```
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, NavigationFragment.newInstance(35.756983, 51.362269));
        transaction.commit();
```

### License
Copyright 2016 Majid Mohammadnejad

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

```
        http://www.apache.org/licenses/LICENSE-2.0
```

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

### Author
Majid Mohammadnejad

Email: m.mohammadnejad@digikala.com

Github: https://github.com/mohammadnejad


