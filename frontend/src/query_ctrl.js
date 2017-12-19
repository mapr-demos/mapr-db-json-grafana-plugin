import {QueryCtrl} from 'app/plugins/sdk';
import './css/query-editor.css!'

export class MapRDBJSONTableDatasourceQueryCtrl extends QueryCtrl {


  constructor($scope, $injector)  {
    super($scope, $injector);

    this.scope = $scope;
    // this.target.target = this.target.target || 'select metric';
    this.target.type = this.target.type || 'Raw Document';
    this.target.limit = this.target.limit || 500;

    this.tableUpdated();
    this.conditionUpdated();
  }

  conditionUpdated() {
    var newJson = this.target.condition || '';
    if (this.conditionOld && newJson !== this.conditionOld) {
        this.refresh();
    }

    this.conditionOld = newJson;
  }

  tableUpdated() {
    var newTable = this.target.table || '';
    if (this.tableOld && newTable !== this.tableOld) {
        this.refresh();
    }

    this.tableOld = newTable;
  }

  getOptions(query) { // todo
    return this.datasource.metricFindQuery(query || '');
  }

  toggleEditorMode() {
    this.target.rawQuery = !this.target.rawQuery;
  }

  onChangeInternal() {
    this.panelCtrl.refresh(); // Asks the panel to refresh data.
  }
}

MapRDBJSONTableDatasourceQueryCtrl.templateUrl = 'partials/query.editor.html';

