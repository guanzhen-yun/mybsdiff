#版本号
cmake_minimum_required(VERSION 3.10.2)

# 查找文件系统中指定模块的路径
file(GLOB bzip_source ${CMAKE_SOURCE_DIR}/bzip/*.c)

# 设置本地的动态库，编译生成
add_library(
             # 模块名(缩写名)
             native-lib

             #动态库
             SHARED

             #源文件
             native-lib.cpp
             bspatch.c
             ${bzip_source})


# 系统库，Log日志
find_library(
              log-lib

              log )

# 目标链接库
target_link_libraries(
                       native-lib

                       ${log-lib} )