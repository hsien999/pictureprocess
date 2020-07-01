const option = ({
    color: ['#3398DB'],
    title: {
        text: '灰度图像素统计直方图',
        subtext: '数据来自HBase数据库',
        left: 'center',
        align: 'right'
    },
    grid: {
        bottom: 80
    },
    toolbox: {
        feature: {
            dataZoom: {
                yAxisIndex: 'none'
            },
            restore: {},
            saveAsImage: {}
        }
    },
    tooltip: {
        trigger: 'axis',
        axisPointer: {
            type: 'cross',
            animation: false,
            label: {
                backgroundColor: '#505765'
            }
        }
    },
    legend: {
        data: ['像素统计值'],
        left: 10
    },
    dataZoom: [
        {
            show: true,
            realtime: true,
            start: 0,
            end: 100
        },
        {
            type: 'inside',
            realtime: true,
            start: 0,
            end: 100
        }
    ],
    yAxis: [
        {
            name: '像素统计值',
            type: 'value',
            min: 0
        }
    ],
    xAxis: [
        {
            name: '灰度',
            type: 'category',
            boundaryGap: false,
            axisLine: {onZero: false},
            data: []
        }
    ],
    series: [
        {
            name: '像素统计值',
            type: 'bar',
            animation: false,
            areaStyle: {},
            lineStyle: {
                width: 1
            },
            data: []
        }
    ]
})
let picture = {
    rowKey: "",
    fileName: "",
    pixelArray: [],
    width: 0,
    height: 0,
    bitCount: 0,
}
let pointsResult = [
    {
        "width": 0,
        "height": 0
    },
    {
        "fileName": '',
        "points": []
    }
]
let pointsResult2 = [
    {
        "fileName": '',
        "similarity": 0.0,
        "matrix": ''
    }
]